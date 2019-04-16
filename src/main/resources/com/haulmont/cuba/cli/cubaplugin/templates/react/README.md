## CUBA React front-end client

#### Overview

React client is alternative to Polymer UI which provides more opionated and front-end oriented
development experience. While it requires better knowledge of modern front-end stack it allows to build 
more complex and feature-rich apps as well as using bigger amount of front-end packages. Unlike 
Polymer which uses outdated bower packaging, React client utilizes more widespread npm ecosystem.
 
#### Technologies

The client uses [TypeScript](http://www.typescriptlang.org/) language. 
It is based on the following frameworks and libraries: 

* [React](https://reactjs.org/) - UI rendering;
* [MobX](https://mobx.js.org/) - reactive state management;
* [Ant Design](https://ant.design/docs/react/introduce) - UI components;
* [React Router](https://reacttraining.com/react-router/) - routing;
* [CUBA REST JS](https://github.com/cuba-platform/cuba-rest-js) - interaction with СUBA generic REST API;

#### Hot deploy and dev server

This type of client does not support hot deploy to Tomcat since React's JSX and Typescript require 
compilation. Instead use node-based dev server which is more convenient and provides live reload 
and hot module replacement. 

It can be easily run via gradle:

```bash
./gradlew npm_run_start
```

Or directly from command line if you have Node.js/npm installed in your dev environment:

```bash
npm run start
``` 

The server will be run in a different port (typically 3000).

#### Configuration

By default client deployed to Tomcat is build with production preset and aimed to be served under 
`app-front` context. Use `PUBLIC_URL` env variable to change this behavior (see `.env.production.local`).

The client served from development server has absolute URL of REST API specified in `REACT_APP_CUBA_URL` 
(see `.env.development.local`).

See `src/config.ts` for full list of common application settings used in runtime.