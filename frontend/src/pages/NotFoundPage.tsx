import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div className="notFoundWrap">
      <p className="eyebrow">404</p>
      <h1>Page not found</h1>
      <p>The requested route does not exist in this frontend application.</p>
      <Link className="primaryButton" to="/">
        Go Home
      </Link>
    </div>
  );
}
