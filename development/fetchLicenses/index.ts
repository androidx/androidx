/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import express = require('express');
import bodyParser = require('body-parser');
import { log, profile } from './logger';
import { PORT } from './flags';
import { handleRequest as handleLicenseRequest } from './license';

/**
 * The HTTP request handler.
 */
class RequestHandler {
  @profile
  defaultHandler(request: express.Request, response: express.Response) {
    response.status(200).send('Server is up.');
  }

  @profile
  async licenseRequestHandler(request: express.Request, response: express.Response) {
    return handleLicenseRequest(request, response);
  }
}

// Bootstrap application.

const app = express();
// define the standard body parsers
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

const requestHandler = new RequestHandler();

app.get('/', requestHandler.defaultHandler);
app.post('/', requestHandler.defaultHandler);
app.post('/convert/licenses', requestHandler.licenseRequestHandler);

app.listen(PORT);
log(`Server started. Listening for requests. Listening on PORT ${PORT}`);
