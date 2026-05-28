import { ReactNode } from "react";

type Props = {
  children: ReactNode;
};

export function AppShell({ children }: Props) {
  return (
    <div className="appFrame">
      <header className="topNav reveal">
        <div className="brandWrap">
          <span className="brandDot" />
          <div>
            <p className="brandName">TrimLink Console</p>
            <p className="brandSub">Distributed URL Shortening Control Plane</p>
          </div>
        </div>
        <a className="navLink" href="https://github.com" target="_blank" rel="noreferrer">
          Docs
        </a>
      </header>
      <main>{children}</main>
    </div>
  );
}
