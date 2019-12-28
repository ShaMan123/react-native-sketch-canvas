/**
 * Metro configuration for React Native
 * https://github.com/facebook/react-native
 *
 * @format
 */

const blacklist = require('metro-config/src/defaults/blacklist');
const { mergeConfig } = require('metro-config');
const path = require('path');
const pkg = require('./package.json');
const baseConfig = require('../base/metro.config');
const _ = require('lodash');
const { useLocalReanimatedModule, reanimatedLocalPath, useBaseImplementation, resolveReanimatedPath, resolveGestureHandlerPath, logInfo } = require('./example.config');

const watchFolders = [path.resolve(__dirname, '..')];
const blacklisters = [];  //path.resolve(__dirname, '../node_modules'),

const REANIMATED = 'react-native-reanimated';
const GH = 'react-native-gesture-handler';
const extraNodeModules = { [REANIMATED]: resolveReanimatedPath(), [GH]: resolveGestureHandlerPath(false) };

logInfo();

if (useBaseImplementation) {
  blacklisters.concat(
    path.resolve(__dirname, 'node_modules', REANIMATED),
  );
} else if (useLocalReanimatedModule) {
  watchFolders.push(path.resolve(__dirname, '..', '..', REANIMATED));
  blacklisters.concat(
    path.resolve(__dirname, '..', '..', REANIMATED, 'node_modules'),
    path.resolve(__dirname, '..', '..', REANIMATED, 'Example')
  );
}

const config = {
  resolver: {
    blacklistRE: blacklist(blacklisters),
    extraNodeModules
  },
  watchFolders,
  transformer: {
    getTransformOptions: async () => ({
      transform: {
        experimentalImportSupport: false,
        inlineRequires: false,
      },
    }),
  },
};


module.exports = useBaseImplementation ? mergeConfig(config, baseConfig) : config;