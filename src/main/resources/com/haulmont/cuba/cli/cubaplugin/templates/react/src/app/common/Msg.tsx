import {AppStateObserver, injectAppState} from "../AppState";
import {observer} from "mobx-react";
import * as React from "react";

type Props = AppStateObserver & {
  entityName: string;
  propertyName: string;
}

export const Msg = injectAppState(observer(({entityName, propertyName, appState}: Props) => {
  if (appState == null || appState.messages == null) {
    return <>propertyName</>;
  }
  const {messages} = appState;
  const message:string = messages[entityName + '.' + propertyName];
  return message != null
    ? <>{message}</>
    : <>{propertyName}</>
}));