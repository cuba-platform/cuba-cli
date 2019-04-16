import * as React from "react";
import {ReactNode} from "react";

export default function Centered({children}: {children?: ReactNode}) {
  return (
    <div style={{width: '100vw', height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
      {children}
    </div>
  )
}