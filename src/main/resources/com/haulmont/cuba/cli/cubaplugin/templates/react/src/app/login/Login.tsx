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

import * as React from "react";
import {ChangeEvent, FormEvent} from "react";
import {Button, Form, Icon, Input, message} from "antd";
import {observer} from "mobx-react";
import {action, observable} from "mobx";
import {AppStateObserver, injectAppState} from "../AppState";

import './Login.css';
import logo from './logo.png';

@injectAppState
@observer
class Login extends React.Component<AppStateObserver> {

  @observable login: string;
  @observable password: string;
  @observable performingLoginRequest = false;

  @action
  changeLogin = (e: ChangeEvent<HTMLInputElement>) => {
    this.login = e.target.value;
  };

  @action
  changePassword = (e: ChangeEvent<HTMLInputElement>) => {
    this.password = e.target.value;
  };

  @action
  doLogin = (e: FormEvent) => {
    e.preventDefault();
    this.performingLoginRequest = true;
    this.props.appState!.login(this.login, this.password)
      .then(action(() => {
        this.performingLoginRequest = false;
      }))
      .catch(action(() => {
        this.performingLoginRequest = false;
        message.error('login failed');
      }));
  };

  render() {
    return(
      <div className='Login'>
        <img src={logo} alt='logo' className='logo'/>
        <div className='login-title'>
          ${project.name}
        </div>
        <Form layout='vertical' onSubmit={this.doLogin}>
          <Form.Item>
            <Input placeholder='Login'
                   onChange={this.changeLogin}
                   value={this.login}
                   prefix={<Icon type="user" style={{ color: 'rgba(0,0,0,.25)' }}/>}
                   size='large'/>
          </Form.Item>
          <Form.Item>
            <Input placeholder='Password'
                   onChange={this.changePassword}
                   value={this.password}
                   type='password'
                   prefix={<Icon type="lock" style={{ color: 'rgba(0,0,0,.25)' }}/>}
                   size='large'/>
          </Form.Item>
          <Form.Item>
            <Button type='primary'
                    htmlType='submit'
                    size='large'
                    block={true}
                    loading={this.performingLoginRequest}>
              Submit
            </Button>
          </Form.Item>
        </Form>
      </div>
    );
  }
}

export default Login;