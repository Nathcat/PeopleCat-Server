import WebSocket from "isomorphic-ws";

export const ws = new WebSocket("ws://localhost:1234");

ws.onopen = () => {
  ws.send("Hello from Node.js");
  ws.close();
};
