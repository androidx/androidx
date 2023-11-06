/* Stub worker. */

import { expose } from "comlink";
import { StatService } from "./service.js";

// This is always running in the context of a Web Worker.
declare var self: Worker;

const service = new StatService();
expose(service, self);
