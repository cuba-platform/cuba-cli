import {action, autorun, computed, IObservableArray, IObservableObject, observable} from "mobx";
import {EnumInfo, MetaClassInfo, MetaPropertyInfo, PermissionInfo, UserInfo} from "@cuba-platform/rest/dist-node/model";
import {inject, IReactComponent, IWrappedComponent} from "mobx-react";
import {cubaREST} from "../index";

export type PropertyType = 'string' | 'int' | 'date' | 'dateTime' | 'boolean';

export class AppState {

  static NAME = 'appState';

  @observable initialized = false;
  @observable authenticated = false;
  @observable usingAnonymously = false;
  @observable userName: string | null;

  @observable permissions: IObservableArray<PermissionInfo>;
  @observable metadata: IObservableArray<MetaClassInfo>;
  @observable messages: IObservableObject;
  @observable enums: IObservableArray<EnumInfo>;

  constructor() {

    autorun(() => {
      if (this.initialized && (this.authenticated || this.usingAnonymously)) {
        cubaREST.getPermissions()
          .then(action((perms: PermissionInfo[]) => {
            this.permissions = observable(perms);
          }));

        cubaREST.loadEnums()
          .then(action((enums: EnumInfo[]) => {
            this.enums = observable(enums);
          }));

        cubaREST.loadMetadata()
          .then(action((metadata: MetaClassInfo[]) => {
            this.metadata = observable(metadata);
          }));

        cubaREST.loadEntitiesMessages()
          .then(action((res) => {
            this.messages = observable(res);
          }))
      }
    })
  }

  @computed get loginRequired(): boolean {
    return !this.authenticated && !this.usingAnonymously;
  }

  @action login(login: string, password: string) {
    return cubaREST.login(login, password).then(action(() => {
      this.userName = login;
      this.authenticated = true;
    }))
  }

  @action
  logout(): Promise<void> {
    if (this.usingAnonymously) {
      this.usingAnonymously = false;
      return Promise.resolve();
    }
    if (cubaREST.restApiToken != null) {
      return cubaREST.logout()
        .then(action(() => {
          this.authenticated = false;
        }));
    }
    return Promise.resolve();
  }

  initialize() {
    cubaREST.getUserInfo()
      .then(action((userInfo: UserInfo) => {
        if (cubaREST.restApiToken == null) {
          this.usingAnonymously = true;
        } else {
          this.authenticated = true;
        }
        this.userName = userInfo.name;
        this.initialized = true;
      }))
      .catch(action(() => {
        this.initialized = true;
      }));
  }

}

export function getPropertyInfo(metadata: MetaClassInfo[], entityName: string, propertyName: string): MetaPropertyInfo | null {
  const metaClass = metadata.find(mci => mci.entityName === entityName);
  if (metaClass == null) {
    return null;
  }
  const propInfo = metaClass.properties.find(prop => prop.name === propertyName);
  return propInfo || null
}

export interface AppStateObserver {
  appState?: AppState;
}

export function injectAppState<T extends IReactComponent>(target: T): T & IWrappedComponent<T> {
  return inject(AppState.NAME)(target);
}