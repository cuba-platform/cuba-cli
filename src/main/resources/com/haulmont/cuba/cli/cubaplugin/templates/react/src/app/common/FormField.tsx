import * as React from "react";
import {Checkbox, DatePicker, Input, Select} from "antd";
import {observer} from "mobx-react";
import {AppStateObserver, getPropertyInfo, injectAppState, PropertyType} from "../AppState";
import {EnumInfo, EnumValueInfo} from "@cuba-platform/rest"

type Props = AppStateObserver & {
  entityName: string;
  propertyName: string;
}

export const FormField = injectAppState(observer(({entityName, propertyName, appState, ...rest}: Props) => {
  if (appState == null || appState.metadata == null) {
    return <Input {...rest}/>;
  }
  const propertyInfo = getPropertyInfo(appState!.metadata, entityName, propertyName);
  if (propertyInfo == null) {
    return <Input {...rest}/>
  }
  switch (propertyInfo.attributeType) {
    case 'ENUM':
      return <EnumField enumClass={propertyInfo.type} {...rest}/>;
    case 'ASSOCIATION':
    case 'COMPOSITION':
      return <Select/>;
  }
  switch (propertyInfo.type as PropertyType) {
    case 'boolean':
      return <Checkbox {...rest}/>;
    case 'date':
      return <DatePicker {...rest}/>;
    case 'dateTime':
      return <DatePicker showTime={true} {...rest}/>;
  }
  return <Input {...rest}/>;
}));


export const EnumField = injectAppState(observer(({enumClass, appState, ...rest}) => {
  let enumValues: EnumValueInfo[] = [];
  if (appState!.enums != null) {
    const enumInfo = appState!.enums.find((enm: EnumInfo) => enm.name === enumClass);
    if (enumInfo != null) {
      enumValues = enumInfo.values;
    }
  }
  return <Select {...rest}>
    {enumValues.map(enumValue =>
      <Select.Option key={enumValue.name} value={enumValue.name}>{enumValue.caption}</Select.Option>
    )}
  </Select>
}));