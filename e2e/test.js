const mineflayer = require('mineflayer');
const { execSync } = require('child_process');

const BOT_NAME = "e2ebot";
const OP_NAME = "e2etest1";
const NON_OP_NAME = "e2etest2";
const SERVER_HOST = "127.0.0.1";
const SERVER_PORT = 25565;

console.log("Starting End-to-End (E2E) test suite...");

function runRcon(command) {
    try {
        console.log(`[RCON] Executing: /${command}`);
        return execSync(`docker exec mcserver rcon-cli ${command}`, { encoding: 'utf8' });
    } catch (e) {
        console.error(`[RCON] Failed to execute command: /${command}`);
        process.exit(1);
    }
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function createBot(username) {
    return mineflayer.createBot({
        host: SERVER_HOST,
        port: SERVER_PORT,
        username,
        version: '1.21.1'
    });
}

function attemptConnection(username, expectedResult, expectedMessageSubstring = "") {
    return new Promise((resolve, reject) => {
        let timeout;
        let retrying = false;
        let quitting = false;

        function connect(attempt = 1) {
            timeout = setTimeout(() => {
                reject(new Error("Connection timed out after 30 seconds."));
            }, 30000);

            const bot = createBot(username);

            bot.on('spawn', () => {
                clearTimeout(timeout);
                quitting = true;
                bot.quit();
                if (expectedResult === 'success') {
                    resolve("Successfully connected.");
                } else {
                    reject(new Error("Bot connected successfully, but was expected to be kicked."));
                }
            });

            bot.on('kicked', (reason) => {
                const reasonStr = String(reason);
                if (reasonStr.includes("Connection throttled") && attempt < 6) {
                    clearTimeout(timeout);
                    retrying = true;
                    setTimeout(() => connect(attempt + 1), 4500);
                    return;
                }
                clearTimeout(timeout);
                if (expectedResult === 'kicked') {
                    if (expectedMessageSubstring && !reasonStr.includes(expectedMessageSubstring)) {
                        reject(new Error(`Bot kicked, but reason '${reasonStr}' did not contain expected substring '${expectedMessageSubstring}'`));
                    } else {
                        resolve(`Kicked correctly: ${reasonStr}`);
                    }
                } else {
                    reject(new Error(`Bot was kicked unexpectedly: ${reasonStr}`));
                }
            });
        
            bot.on('end', (reason) => {
                if (expectedResult === 'success' && !retrying && !quitting) {
                    clearTimeout(timeout);
                    reject(new Error(`Bot disconnected unexpectedly: ${reason}`));
                }
            });

            bot.on('error', (err) => {
                clearTimeout(timeout);
                reject(err);
            });
        }

        connect();
    });
}

function runPlayerCommands(username, commands) {
    return new Promise((resolve, reject) => {
        const receivedMessages = [];
        let commandIndex = 0;
        let bot;
        let timeout;
        let retrying = false;
        let quitting = false;

        function resetTimeout() {
            if (timeout) clearTimeout(timeout);
            timeout = setTimeout(() => {
                const cmd = commands[commandIndex] ? commands[commandIndex].command : "unknown";
                reject(new Error(`Timed out waiting for response to /${cmd}. Received: ${receivedMessages.join(" | ") || "<none>"}`));
            }, 30000);
        }

        function sendNextCommand() {
            if (commandIndex >= commands.length) {
                clearTimeout(timeout);
                quitting = true;
                bot.quit();
                resolve();
                return;
            }
            resetTimeout();
            bot.chat(`/${commands[commandIndex].command}`);
        }

        function connect(attempt = 1) {
            resetTimeout();
            bot = createBot(username);
            
            bot.once('spawn', sendNextCommand);
            
            bot.on('message', (message) => {
                const text = message.toString().replace(/§[0-9a-fk-or]/gi, '');
                receivedMessages.push(text);
                if (commandIndex >= commands.length) return;
                
                const expected = commands[commandIndex].expectedMessageSubstring;
                if (!text.toLowerCase().includes(expected.toLowerCase())) {
                    return;
                }
                commandIndex += 1;
                setTimeout(sendNextCommand, 250);
            });

            bot.on('kicked', (reason) => {
                const reasonStr = String(reason);
                if (reasonStr.includes("Connection throttled") && attempt < 6) {
                    clearTimeout(timeout);
                    retrying = true;
                    setTimeout(() => connect(attempt + 1), 4500);
                    return;
                }
                clearTimeout(timeout);
                reject(new Error(`${username} was kicked while running commands: ${reasonStr}`));
            });

            bot.on('error', (err) => {
                clearTimeout(timeout);
                reject(err);
            });
            
            bot.on('end', (reason) => {
                if (!retrying && !quitting) {
                    clearTimeout(timeout);
                    reject(new Error(`Bot disconnected unexpectedly: ${reason}`));
                }
            });
        }

        connect();
    });
}

function runPlayerCommand(username, command, expectedMessageSubstring) {
    return runPlayerCommands(username, [{ command, expectedMessageSubstring }]);
}

async function runTestSuite() {
    try {
        console.log("--- Test 0: Plugin Loaded ---");
        const plugins = runRcon("plugins");
        if (!plugins.includes("PremiumUUID")) {
            throw new Error(`PremiumUUID is not loaded by the server. RCON response: ${plugins.trim()}`);
        }
        console.log("[Test 0] Passed.\n");

        console.log("--- Test 1: Initial Connection ---");
        await attemptConnection(BOT_NAME, 'success');
        console.log("[Test 1] Passed.\n");

        console.log("--- Test 2: Ban Enforcement ---");
        runRcon(`ban ${BOT_NAME} "Banned by E2E test"`);
        await sleep(1000);
        await attemptConnection(BOT_NAME, 'kicked', "Banned by E2E test");
        console.log("[Test 2] Passed.\n");

        console.log("--- Test 3: Pardon Interception Enforcement ---");
        runRcon(`pardon ${BOT_NAME}`);
        await sleep(2000); // Provide time for the plugin's 1-tick delay cleanup
        await attemptConnection(BOT_NAME, 'success');
        console.log("[Test 3] Passed.\n");

        console.log("--- Test 4: Whitelist Enforcement ---");
        runRcon("whitelist on");
        await sleep(1000);
        await attemptConnection(BOT_NAME, 'kicked', "whitelisted");
        console.log("[Test 4] Passed.\n");

        console.log("--- Test 5: Whitelist Admission ---");
        runRcon(`whitelist add ${BOT_NAME}`);
        await sleep(1000);
        await attemptConnection(BOT_NAME, 'success');
        console.log("[Test 5] Passed.\n");

        console.log("--- Test 6: Non-OP Command Permission ---");
        runRcon("whitelist off");
        await sleep(1000);
        runRcon(`deop ${NON_OP_NAME}`);
        await runPlayerCommand(NON_OP_NAME, "premiumuuid reload", "unknown or incomplete command");
        console.log("[Test 6] Passed.\n");

        console.log("--- Test 7: OP Commands ---");
        runRcon(`op ${OP_NAME}`);
        await runPlayerCommand(OP_NAME, "premiumuuid reload", "config reloaded");
        console.log("[Test 7] Passed.\n");

        console.log("--- Test 8: Status and Cache Clearing ---");
        await attemptConnection(OP_NAME, 'success');
        await sleep(1000);
        await runPlayerCommands(OP_NAME, [
            { command: `premiumuuid status ${OP_NAME}`, expectedMessageSubstring: `Cache: ${OP_NAME}` },
            { command: `premiumuuid clearcache ${OP_NAME}`, expectedMessageSubstring: `Cache cleared for '${OP_NAME}'.` },
            { command: `premiumuuid status ${OP_NAME}`, expectedMessageSubstring: `No cache entry for '${OP_NAME}'.` },
            { command: "premiumuuid clearcache", expectedMessageSubstring: "Entire UUID cache cleared." }
        ]);
        console.log("[Test 8] Passed.\n");

        console.log("--- Test 9: Enable Override ---");
        await runPlayerCommand(OP_NAME, `puuid enable ${NON_OP_NAME}`, `Override for '${NON_OP_NAME}' set to enabled.`);
        await attemptConnection(NON_OP_NAME, 'success');
        await sleep(1000);
        await runPlayerCommand(OP_NAME, `premiumuuid status ${NON_OP_NAME}`, `Override: active`);
        console.log("[Test 9] Passed.\n");

        console.log("--- Test 10: Disable Override ---");
        await runPlayerCommand(OP_NAME, `premiumuuid disable ${NON_OP_NAME}`, `Override for '${NON_OP_NAME}' set to disabled.`);
        await runPlayerCommand(OP_NAME, `premiumuuid status ${NON_OP_NAME}`, `Override: inactive`);
        console.log("[Test 10] Passed.\n");

        console.log("All E2E tests completed successfully.");
        process.exit(0);

    } catch (error) {
        console.error("E2E Test Suite Failed:", error.message);
        process.exit(1);
    }
}

runTestSuite();
