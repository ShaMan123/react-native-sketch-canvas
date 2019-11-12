'use strict';

import * as _ from 'lodash';
import React, { forwardRef, Ref, useCallback, useEffect, useImperativeHandle, useMemo, useRef } from 'react';
import { findNodeHandle, LayoutChangeEvent, NativeSyntheticEvent, processColor, requireNativeComponent, UIManager } from 'react-native';
import { requestPermissions } from './handlePermissions';
import { useModule } from './SketchCanvasModule';
import { CanvasText, Commands, Path, PathData, SketchCanvasProperties, SketchCanvasRef } from './types';

const RNSketchCanvas = requireNativeComponent('RNSketchCanvas');
//const { Commands } = UIManager.getViewManagerConfig('RNSketchCanvas');

export function generatePathId() {
  return _.uniqueId('SketchCanvasPath');
}

function processText(text: CanvasText[] | null) {
  text && text.forEach(t => t.fontColor = processColor(t.fontColor));
  return text;
}

function useRefGetter<T, R = T>(initialValue?: T, action: (ref: T) => R = (current) => (current as unknown as R)) {
  const ref = useRef(initialValue);
  const getter = useCallback(() =>
    action(ref.current as T),
    [ref, action]
  );
  const defaultGetter = useCallback(() =>
    ref.current,
    [ref]
  );

  return [ref, getter, defaultGetter] as [typeof ref, typeof getter, typeof defaultGetter];
}

function SketchCanvas(props: SketchCanvasProperties, forwardedRef: Ref<SketchCanvasRef>) {
  const {
    onLayout: onLayoutP,
    onPathsChange,
    onSketchSaved,
    strokeColor: strokeColorP,
    strokeWidth,
    text: textP,
    permissionDialogMessage,
    permissionDialogTitle,
    user
  } = props;

  const strokeColor = useMemo(() => processColor(strokeColorP), [strokeColorP]);
  const text = useMemo(() =>
    processText(textP ? textP.map(t => Object.assign({}, t)) : null),
    [textP]
  );

  const [_initialized, isInitialized] = useRefGetter(false);
  const [_size, size] = useRefGetter({ width: 0, height: 0 });
  const [_ref, handle, ref] = useRefGetter(null as any, (current) => findNodeHandle(current));
  const [_currentPath, currentPath] = useRefGetter<string>();
  const [_paths, paths] = useRefGetter([] as Path[]);
  const [_path, path] = useRefGetter({} as PathData);
  const [_pathsToProcess, pathsToProcess] = useRefGetter<Path[]>([]);

  const dispatchCommand = useCallback((command: Commands, data: any[] = []) =>
    UIManager.dispatchViewManagerCommand(handle(), command, data),
    [handle]
  );

  const module = useModule(handle());

  const findPath = useCallback((pathId: string) =>
    _.find(paths(), (p) => _.isEqual(p.path.id, pathId)),
    [paths]
  );

  const addPaths = useCallback((data: Path[]) => {
    if (isInitialized()) {
      const parsedPaths = data.map((data) => {
        if (_.isNil(findPath(data.path.id))) paths().push(data);
        const scaler = size().width / data.size.width;
        return [
          data.path.id,
          processColor(data.path.color),
          data.path.width,
          _.map(data.path.points, (p) => {
            return _.mapValues(p, (val) => val * scaler);
          })
        ];
      });

      dispatchCommand(Commands.addPaths, parsedPaths);
    }
    else {
      data.map((data) => pathsToProcess().filter(p => p.path.id === data.path.id).length === 0 && pathsToProcess().push(data));
    }
  },
    [isInitialized, dispatchCommand, findPath, paths, pathsToProcess, size]
  );

  const addPath = useCallback((data: Path) =>
    addPaths([data]),
    [addPaths]
  );

  const deletePaths = useCallback((pathIds: string[]) => {
    _paths.current = paths()
      .filter(p => pathIds.findIndex(id => p.path.id === id) === -1);
    dispatchCommand(Commands.deletePaths, pathIds);
  }, [_paths, paths, dispatchCommand]);

  const deletePath = useCallback((id: string) =>
    deletePaths([id]),
    [deletePaths]
  );

  const getPaths = useCallback(() =>
    _.cloneDeep(paths()),
    [paths]
  );

  const startPath = useCallback((x: number, y: number) => {
    _currentPath.current = generatePathId();
    const state = {
      id: currentPath(),
      color: strokeColor,
      width: strokeWidth,
      //points: []
    };

    dispatchCommand(Commands.newPath, _.values(state));
    //props.onStrokeStart && props.onStrokeStart(path());
    /*
    _path.current = {
      id: generatePathId(),
      color: strokeColor,
      width: strokeWidth,
      points: []
    };

    const data = [
      path().id,
      processColor(path().color),
      path().width
    ]

    dispatchCommand(commands.newPath, data);
    props.onStrokeStart && props.onStrokeStart(path());
    */
  },
    [_path, path]
  );

  const addPoint = useCallback((x: number, y: number) => {
    if (currentPath()) {
      dispatchCommand(Commands.addPoint, [x, y]); //parseFloat(x.toFixed(2))
      //this._path.points.push({ x: pointX, y: pointY });
      //this.props.onStrokeChanged && this.props.onStrokeChanged({ x: pointX, y: pointY, id: this._path.id });
    }
  },
    [currentPath, dispatchCommand]
  );

  const endPath = useCallback(() => {
    if (currentPath()) {
      dispatchCommand(Commands.endPath);
      _currentPath.current = undefined;
      /*
      const o = {
        path: this._path,
        size: this._size,
        drawer: this.props.user
      };
      this._paths.push(o);
      this.props.onStrokeEnd && this.props.onStrokeEnd(this._path);
      this._path = null;
      */
    }
  },
    [_currentPath, currentPath, dispatchCommand]
  );

  const clear = useCallback(() => {
    _paths.current = [];
    _path.current = null;
    _currentPath.current = undefined;
    dispatchCommand(Commands.clear);
  }, [_paths, _path, _currentPath]);

  const undo = useCallback(() => {
    let lastId: string | null = null;
    paths().forEach(d => lastId = d.drawer === user ? d.path.id : lastId);
    if (lastId !== null) deletePath(lastId);
    return lastId;
  }, [paths, user, deletePath]);

  const setNativeProps = useCallback((props) =>
    ref() && ref().setNativeProps(props),
    [ref]
  );

  const onLayout = useCallback((e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    _size.current = { width, height }
    _initialized.current = true;
    pathsToProcess().length > 0 && addPaths(pathsToProcess());
    onLayoutP && onLayoutP(e);
  }, [_size, _initialized, pathsToProcess, onLayoutP]);

  const onChange = useCallback((e: NativeSyntheticEvent<any>) => {
    if (!isInitialized()) return;
    if (e.nativeEvent.hasOwnProperty('pathsUpdate') && onPathsChange) {
      //onPathsChange(e);
    } else if (e.nativeEvent.hasOwnProperty('success') && onSketchSaved) {
      onSketchSaved(e);
    }
  }, [onPathsChange, onSketchSaved, isInitialized]);
  
  useEffect(() => {
    requestPermissions(
      permissionDialogTitle,
      permissionDialogMessage,
    ).then((isStoragePermissionAuthorized) => isStoragePermissionAuthorized);
  }, []);

  useImperativeHandle(forwardedRef, () =>
    ({
      ...module,
      addPath,
      addPaths,
      deletePath,
      deletePaths,
      getPaths,
      startPath,
      addPoint,
      endPath,
      clear,
      undo,
      setNativeProps,
      getNode: ref,
      handle
    }),
    [
      module,
      addPath,
      addPaths,
      deletePath,
      deletePaths,
      getPaths,
      startPath,
      addPoint,
      endPath,
      clear,
      undo,
      setNativeProps,
      ref,
      handle
    ]
  );

  return (
    <RNSketchCanvas
      {...props}
      ref={_ref}
      onLayout={onLayout}
      onChange={onChange}
      text={text}
      strokeColor={strokeColor}
    />
  )
}

const ForwardedSketchCanvas = forwardRef(SketchCanvas);
ForwardedSketchCanvas.defaultProps = {
  strokeColor: '#000000',
  strokeWidth: 3,
  touchEnabled: true,
  
  permissionDialogTitle: '',
  permissionDialogMessage: '',

  hardwareAccelerated: false
} as SketchCanvasProperties;
ForwardedSketchCanvas.displayName = '() => SketchCanvas'

export default ForwardedSketchCanvas;
