/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import App from './app/App';
// import registerServiceWorker from './registerServiceWorker';
import {Provider} from "mobx-react";
import {AppState} from "./app/AppState";

import {HashRouter, Route} from "react-router-dom";
import {initializeApp} from "@cuba-platform/rest";
import {CUBA_APP_URL} from "./config";

import 'antd/dist/antd.css';
import './index.css';

export const cubaREST = initializeApp({
  name: '${react.restName}',
  apiUrl: CUBA_APP_URL,
  storage: window.localStorage
});

const appState = new AppState();
appState.initialize();


ReactDOM.render(
  <Provider appState={appState}>
    <HashRouter>
      <Route component={App}/>
    </HashRouter>
  </Provider>
  ,
  document.getElementById('root') as HTMLElement
);
// registerServiceWorker();