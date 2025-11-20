/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <android/log.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>
#include <limits.h>
#include <stdint.h>

// Tag for Android logs
#define LOG_TAG "NativeShellProcess"

// Global verbose logs
int verboselogs = 0;

// Define for logging function.
// LOGI is printed only when verbose logs is on, when starting the cli
#define LOGI(...) if (verboselogs == 1) { \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__); \
};
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// File descriptors for the server sockets. The server sockets are owned by the parent process,
// i.e. the server.
int serverStdinFd = 0;
int serverStdoutFd = 0;
int serverStderrFd = 0;

// Global shutdown flag that triggers the main server shutdown.
// It's attached to SIGTERM and SIGINT -> when the signal is sent this flag is flipped to 1.
volatile sig_atomic_t serverShutdown = 0;

/**
 * Creates a server socket on the given port. This is used to create the 3 server sockets on which
 * the parent will listen for incoming connections.
 * @param port an integer to use as server socket port.
 * @return the file descriptor for the socket.
 */
int createSocket(uint16_t port) {

    // Create socket address
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(struct sockaddr_in));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    // Create socket
    int fd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (fd == -1) {
        LOGE("socket(AF_INET, SOCK_STREAM) failed: %s", strerror(errno));
        return -1;
    }

    // Allow immediate reuse of the port
    int reuse = 1;
    if (setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse)) == -1) {
        LOGE("setsockopt(SO_REUSEADDR) failed: %s", strerror(errno));
        close(fd);
        return -1;
    }

    // Bind the socket to the address and port
    if (bind(fd, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
        LOGE("bind() to port %u failed: %s", port, strerror(errno));
        close(fd);
        return -1;
    }

    // Start listening for incoming connections
    if (listen(fd, 100) == -1) {
        LOGE("listen() on port %u failed: %s", port, strerror(errno));
        close(fd);
        return -1;
    }

    LOGI("TCP server listening on port %u", port);
    return fd;
}

/**
 * Accepts a connection on a previously created open socket.
 * @param socketFd the socket file descriptor to accept a connection from.
 * @return a file descriptor for the client socket.
 */
int acceptConnection(int socketFd) {
    struct sockaddr_in clientAddr;
    socklen_t clientAddrLen = sizeof(clientAddr);

    // Accept an incoming connection
    int clientFd = accept(socketFd, (struct sockaddr *) &clientAddr, &clientAddrLen);
    if (clientFd == -1) {
        LOGE("accept() failed: %s", strerror(errno));
        return -1;
    }

    // Log client connection information
    char clientIpStr[INET_ADDRSTRLEN];
    if (inet_ntop(AF_INET, &clientAddr.sin_addr, clientIpStr, sizeof(clientIpStr)) != NULL) {
        LOGI("Accepted TCP connection from %s:%u on fd: %d",
             clientIpStr,
             ntohs(clientAddr.sin_port),
             clientFd);
    } else {
        LOGI("Accepted TCP connection (could not get client IP) on fd: %d", clientFd);
    }

    return clientFd;
}

/**
 * Parses the given argument to determine the socket server port and sets the given pointer to the
 * parsed value.
 * @param arg the argument to parse.
 * @param port_out the port argument in output.
 * @return -1 if an error occurred, 0 otherwise.
 */
int parsePortArg(const char *arg, uint16_t *port_out) {
    char *endptr;
    unsigned long parsed_port;

    errno = 0; // Reset errno before strtoul
    parsed_port = strtoul(arg, &endptr, 10);

    // Check for conversion errors or invalid input
    if (errno != 0 || *endptr != '\0' || parsed_port > UINT16_MAX) {
        LOGE("Invalid port number provided: %s", arg);
        return -1;
    }
    *port_out = (uint16_t) parsed_port;
    return 0;
}

/**
 * Triggers the shutdown for the server sockets and clean up and created file.
 */
void shutdownServerSockets() {
    shutdown(serverStdinFd, SHUT_RDWR);
    shutdown(serverStdoutFd, SHUT_RDWR);
    shutdown(serverStderrFd, SHUT_RDWR);

    close(serverStdinFd);
    close(serverStdoutFd);
    close(serverStderrFd);

    remove("/data/local/tmp/process.pid");
}

/**
 * Handler function for shutdown signals to gracefully shutdowns sockets and terminates the process.
 * Registered shutdown signals are SIGTERM and SIGINT.
 * See [setup_signal_handler_shutdown] for setup.
 * @param signal the signal triggered.
 */
void handleSignalShutdown(int signal) {
    LOGI("handle_signal(%d)", signal);

    // Handler for signals sent to the process
    serverShutdown = 1;
    shutdownServerSockets();
}

/**
 * Sets up the shutdown signals, i.e. SIGTERM and SIGINT.
 */
void setupSignalHandlerShutdown() {

    // Set up signal handler function to capture SIGTERM.
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = &handleSignalShutdown;
    sa.sa_flags = 0;
    sigfillset(&sa.sa_mask);

    // Register SIGTERM handler
    if (sigaction(SIGTERM, &sa, NULL) == -1) {
        LOGE("Error setting up SIGTERM handler");
        perror("Error setting up SIGTERM handler");
        exit(1);
    }

    // Register SIGINT handler
    if (sigaction(SIGINT, &sa, NULL) == -1) {
        LOGE("Error setting up SIGINT handler");
        perror("Error setting up SIGINT handler");
        exit(1);
    }
}

/**
 * Handler function for child process shutdown to clean up resources.
 * Without this, dead child processes are marked as zombies and resources never collected.
 * Registered signal is SIGCHLD.
 * See [setup_signal_handler_shutdown] for setup.
 * @param signal the signal triggered.
 */
void handleSignalChildEnd(int signal) {
    LOGI("handle_signal(%d)", signal);
    int status;
    while (waitpid(-1, &status, WNOHANG) > 0) {
        // Just wait
    }
}

/**
 * Sets up the child end signal, i.e. SIGCHLD.
 * This signal is to let the parent process know that the child has terminated, so resources can
 * be cleaned up. Without this, after a child terminates it will marked as a zombie process but
 * still show up when querying whether the process is alive (through ps -p or kill -0, etc).
 */
void setupSignalHandlerChildEnd() {
    struct sigaction sa;
    sa.sa_handler = &handleSignalChildEnd;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_RESTART | SA_NOCLDSTOP;

    // Register SIGCHLD handler
    if (sigaction(SIGCHLD, &sa, 0) == -1) {
        LOGE("Error setting up SIGCHLD handler");
        perror("Error setting up SIGCHLD handler");
        exit(1);
    }
}

/**
 * Main Function.
 */
int main(int argc, char *argv[]) {

    // Socket shell is a server that opens 3 tcp sockets bound on localhost on the ports given as
    // argument. After that it awaits connection on the 3 ports. When the connection is accepted
    // the process is forked and client socket file descriptors are used as stdin, stdout and stderr
    // for the child process. The child process then runs `sh` via `execl` swapping its memory
    // with the newly launched command, but keeping the 3 file descriptors. The result is that the
    // child process will write directly on the tcp sockets. The parent meanwhile can move on and
    // accept a new connection.
    // Note that since the parent doesn't block, when the client dies it goes on a zombie state
    // until the parent cleans up its resources. To do that, the parent listens for SIGCHLD signal
    // that is received when the child dies. In `handle_signal_child_end` the parent can clean up
    // all the child processes resources that are marked as zombies.

    // Check the number of arguments
    if (argc != 5) {
        LOGE("Usage: %s <verbose_logs: 0 or 1> <stdin_socket_port> "
             "<stdout_socket_port> <stderr_socket_port>",
             argv[0]);
        return 1;
    }

    // This is the first parameter and determines whether LOGI are printed or not.
    verboselogs = atoi(argv[1]);

    // Set up signal handling (SIGINT, SIGTERM, SIGCHLD)
    setupSignalHandlerChildEnd();
    setupSignalHandlerShutdown();

    // Get the socket ports and command from the arguments
    uint16_t stdinSocketPort, stdoutSocketPort, stderrSocketPort;
    if (parsePortArg(argv[2], &stdinSocketPort) != 0) return 1;
    if (parsePortArg(argv[3], &stdoutSocketPort) != 0) return 1;
    if (parsePortArg(argv[4], &stderrSocketPort) != 0) return 1;
    LOGI("Starting native shell with stdin port: %u, stdout port: %u, stderr port: %u",
         stdinSocketPort, stdoutSocketPort, stderrSocketPort);

    // Create the sockets, checking for errors after each step
    serverStdinFd = createSocket(stdinSocketPort);
    if (serverStdinFd == -1) {
        return 1;
    }

    serverStdoutFd = createSocket(stdoutSocketPort);
    if (serverStdoutFd == -1) {
        close(serverStdinFd);
        return 1;
    }

    serverStderrFd = createSocket(stderrSocketPort);
    if (serverStderrFd == -1) {
        close(serverStdinFd);
        close(serverStdoutFd);
        return 1;
    }

    // Write the process id in a file named process.pid.
    // This is so that the kotlin library side has an easy way to read the process id, as well
    // know that the server is ready to accept connections.
    printf("%d \n", getpid());
    fflush(stdout);

    // Start server loop
    LOGI("Waiting for incoming connection");
    while (!serverShutdown) {

        // Await and accept connections, checking for errors
        int clientStdinFd = acceptConnection(serverStdinFd);
        if (clientStdinFd == -1) {
            LOGE("stdin acceptConnection failed: %s", strerror(errno));
            continue;
        }

        int clientStdoutFd = acceptConnection(serverStdoutFd);
        if (clientStdoutFd == -1) {
            close(clientStdinFd);
            LOGE("stdout acceptConnection failed: %s", strerror(errno));
            continue;
        }

        int clientStderrFd = acceptConnection(serverStderrFd);
        if (clientStderrFd == -1) {
            close(clientStdoutFd);
            close(clientStdinFd);
            LOGE("stderr acceptConnection failed: %s", strerror(errno));
            continue;
        }

        LOGI("Client connected");

        // Fork the current process
        pid_t childPid = fork();

        // If the process forking failed, clean up resources
        if (childPid == -1) {
            LOGE("process fork failed: %s", strerror(errno));
            close(clientStderrFd);
            close(clientStdoutFd);
            close(clientStdinFd);
            // Failure to fork a process causes server process to die.
            return 1;
        }

        // This execution branch is for the child process
        if (childPid == 0) {

            // Close the socket server file descriptors because the child process
            // doesn't need to listen.
            close(serverStdinFd);
            close(serverStdoutFd);
            close(serverStderrFd);

            // Redirect stdin, stdout, and stderr. Exit immediately if dup2 fails.
            if (dup2(clientStdinFd, STDIN_FILENO) == -1) {
                LOGE("dup2(stdin) failed: %s", strerror(errno));
                exit(1);
            }
            if (dup2(clientStdoutFd, STDOUT_FILENO) == -1) {
                LOGE("dup2(stdout) failed: %s", strerror(errno));
                exit(1);
            }
            if (dup2(clientStderrFd, STDERR_FILENO) == -1) {
                LOGE("dup2(stderr) failed: %s", strerror(errno));
                exit(1);
            }

            // Close the client socket file descriptors because now they're duplicated
            close(clientStdinFd);
            close(clientStdoutFd);
            close(clientStderrFd);

            // Execute the command
            LOGI("Starting child process shell");

            // Use command directly, ensure last arg is NULL
            execl("/system/bin/sh", "sh", (char *) NULL);

            // Note that this code is executed only if execl fails.
            LOGE("execl() failed: %s", strerror(errno));
            exit(1);
        }

        // This execution branch is for the parent process
        if (childPid > 0) {

            // Parent closes its copy of the client sockets immediately after fork
            close(clientStdinFd);
            close(clientStdoutFd);
            close(clientStderrFd);
        }
    }

    shutdownServerSockets();
    LOGI("NativeShellProcess finished");

    return 0;
}
