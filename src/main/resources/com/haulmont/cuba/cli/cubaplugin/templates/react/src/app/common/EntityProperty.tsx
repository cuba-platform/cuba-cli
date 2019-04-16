import * as React from "react";
import {observer} from "mobx-react";
import {AppStateObserver, injectAppState} from "../AppState";

type Props = AppStateObserver & {
  entityName: string;
  propertyName: string;
  showLabel?: boolean;
  hideIfEmpty?: boolean;
  value: any;
}

export const EntityProperty = injectAppState(observer(
  ({
     entityName,
     propertyName,
     value,
     appState,
     showLabel = true,
     hideIfEmpty = true,
   }: Props) => {
    if (hideIfEmpty && value == null) {
      return null;
    }
    if (appState == null || appState.messages == null || !showLabel) {
      return <div>{formatValue(value)}</div>;
    }
    const {messages} = appState;
    const label: string = messages[entityName + '.' + propertyName];
    return label != null
      ? <div><strong>{label}:</strong> {formatValue(value)}</div>
      : <div>{formatValue(value)}</div>
  }));

function formatValue(value: any): string {
  const valType = typeof value;
  if (valType === "string") {
    return value;
  }
  if (valType === "object") {
    if (Object.prototype.hasOwnProperty.call(value, '_instanceName')) {
      return value._instanceName!;
    }
  }
  return JSON.stringify(value);
}