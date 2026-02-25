# androidx SQLite Web Worker

This web worker provides an interface to the SQLite WASM API, allowing the main thread to perform
database operations off the main thread.

## Messaging Protocol

The communication between the driver (main thread) and the worker is done via messages.
All messages, both requests and responses, are wrapped in an object with an `id` field that is used
to correlate requests with responses.

### Request Messages

A request from the driver to the worker has the following structure:

```json
{
  "id": <number>,
  "data": {
    "cmd": "<command_name>",
    ...
  }
}
```

- `id`: A unique identifier for the request.
- `data`: An object containing the command and its parameters.
  - `cmd`: The name of the command to execute.

### Response Messages

A response from the worker to the driver has the following structure:

**Success:**
```json
{
  "id": <number>,
  "data": { ... }
}
```

**Error:**
```json
{
  "id": <number>,
  "error": "<error_message>"
}
```

- `id`: The identifier of the request this response corresponds to.
- `data`: An object containing the result of a successful command.
- `error`: A string message describing the error if the command failed.

## Commands

### `open`

Opens a new database connection. If successful responds with the database connection unique ID.

**Request:**
```json
{
  "cmd": "open",
  "fileName": "<string>"
}
```

**Success Response:**
```json
{
  "databaseId": <number>
}
```

### `prepare`

Prepares a new SQL statement for execution. If successful responds with the statement unique ID and
information about the prepared statement.

**Request:**
```json
{
  "cmd": "prepare",
  "databaseId": <number>,
  "sql": "<string>"
}
```

**Success Response:**
```json
{
  "statementId": <number>,
  "parameterCount": <number>,
  "columnNames": ["<string>", ...]
}
```

### `step`

Executes a prepared statement. Involves binding parameters sent in the request and then stepping
through all the result rows of the statement to respond with their column result.

**Request:**
```json
{
  "cmd": "step",
  "statementId": <number>,
  "bindings": [...]
}
```

**Success Response:**
```json
{
  "rows": [[...], ...],
  "columnTypes": [<number>, ...]
}
```

### `close`

Closes a prepared statement or a database connection. This is a one-way command; no success response
is sent.

**Request:**
```json
{
  "cmd": "close",
  "statementId": <number>,
  "databaseId": <number>
}
```
