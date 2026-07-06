import { StoreDashboard } from "./components/StoreDashboard";
import { config } from "./config";
import "./styles.css";

export function App() {
  return <StoreDashboard bffBaseUrl={config.bffBaseUrl} />;
}
