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

import {Button, Modal} from "antd";
import * as React from "react";
import {AppStateObserver, injectAppState} from "../AppState";
import {observer} from "mobx-react";
import './AppHeader.css';
import logo from './logo.png';

@injectAppState
@observer
class AppHeader extends React.Component<AppStateObserver> {

  render() {
    const appState = this.props.appState!;

    return (
      <div className="AppHeader">
        <div className="logo">
          <img src={logo}/>
        </div>
        <div className="user-info">
          <span>{appState.userName}</span>
          <Button ghost={true}
                  icon='logout'
                  style={{border: 0}}
                  onClick={this.showLogoutConfirm}/>
        </div>
      </div>
    )
  }

  showLogoutConfirm = () => {
    Modal.confirm({
      title: 'Are you sure you want to logout?',
      okText: 'Logout',
      cancelText: 'Cancel',
      onOk: () => {
        this.props.appState!.logout()
      }
    });
  }

}

export default AppHeader;