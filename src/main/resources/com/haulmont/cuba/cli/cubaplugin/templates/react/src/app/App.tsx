import * as React from 'react';
import './App.css';

import {Icon, Layout, Menu} from "antd";
import {AppStateObserver, injectAppState} from "./AppState";
import {observer} from "mobx-react";
import Login from "./login/Login";
import Centered from "./common/Centered";
import AppHeader from "./header/AppHeader";
import {NavLink, Route, Switch} from "react-router-dom";
import HomePage from "./home/HomePage";
import {mainRoutes} from "../routing";

@injectAppState
@observer
class App extends React.Component<AppStateObserver> {

  render() {

    const appState = this.props.appState!;
    const {initialized, loginRequired} = appState;

    if (!initialized) {
      return (
        <Centered>
          <Icon type="loading" style={{fontSize: 24}} spin={true}/>
        </Centered>
      )
    }

    if (loginRequired) {
      return (
        <Centered>
          <Login/>
        </Centered>
      )
    }

    return (
      <Layout className='main-layout'>
        <Layout.Header>
          <AppHeader/>
        </Layout.Header>
        <Layout>
          <Layout.Sider width={200}
                        breakpoint='sm'
                        collapsedWidth={0}
                        style={{background: '#fff'}}>
            <Menu mode="inline"
                  style={{height: '100%', borderRight: 0}}>
              <Menu.Item key="1">
                <NavLink to={'/'}><Icon type="home"/>Home</NavLink>
              </Menu.Item>
              {mainRoutes.map((route) =>
                <Menu.Item key={route.menuLink}>
                  <NavLink to={route.menuLink}><Icon type="bars"/>{route.caption}</NavLink>
                </Menu.Item>
              )}
            </Menu>
          </Layout.Sider>
          <Layout style={{ padding: '24px 24px 24px' }}>
            <Layout.Content>
              <Switch>
                <Route exact={true} path="/" component={HomePage}/>
                {mainRoutes.map((route) =>
                  <Route key={route.pathPattern} path={route.pathPattern} component={route.component}/>
                )}
              </Switch>
            </Layout.Content>
          </Layout>
        </Layout>
      </Layout>
    );
  }
}

export default App;
