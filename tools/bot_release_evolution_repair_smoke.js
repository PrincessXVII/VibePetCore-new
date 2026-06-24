const mineflayer = require(process.env.MINEFLAYER_PATH || 'mineflayer');

const host = process.env.MC_HOST || '127.0.0.1';
const port = Number(process.env.MC_PORT || '25570');
const username = process.env.MC_USER || 'SmokeBot';
const version = process.env.MC_VERSION || '1.21.11';

function log(type, detail = {}) {
  console.log(JSON.stringify({ t: new Date().toISOString(), type, ...detail }));
}

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function waitForEvent(emitter, event, timeoutMs) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      cleanup();
      reject(new Error(`timeout:${event}`));
    }, timeoutMs);

    function cleanup() {
      clearTimeout(timer);
      emitter.removeListener(event, onEvent);
      emitter.removeListener('end', onEnd);
      emitter.removeListener('kicked', onKicked);
      emitter.removeListener('error', onError);
    }

    function onEvent(...args) {
      cleanup();
      resolve(args);
    }

    function onEnd(reason) {
      cleanup();
      reject(new Error(`ended-before-${event}:${reason}`));
    }

    function onKicked(reason) {
      cleanup();
      reject(new Error(`kicked-before-${event}:${reason}`));
    }

    function onError(error) {
      cleanup();
      reject(error);
    }

    emitter.once(event, onEvent);
    emitter.once('end', onEnd);
    emitter.once('kicked', onKicked);
    emitter.once('error', onError);
  });
}

function createChatWaiter(bot, timeoutMs, predicate, label) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      cleanup();
      reject(new Error(`timeout:chat:${label}`));
    }, timeoutMs);

    function cleanup() {
      clearTimeout(timer);
      bot.removeListener('message', onMessage);
      bot.removeListener('end', onEnd);
      bot.removeListener('kicked', onKicked);
      bot.removeListener('error', onError);
    }

    function onMessage(jsonMsg, position) {
      const text = jsonMsg.toString();
      if (predicate(text, position)) {
        cleanup();
        resolve({ text, position });
      }
    }

    function onEnd(reason) {
      cleanup();
      reject(new Error(`ended-before-chat:${label}:${reason}`));
    }

    function onKicked(reason) {
      cleanup();
      reject(new Error(`kicked-before-chat:${label}:${reason}`));
    }

    function onError(error) {
      cleanup();
      reject(error);
    }

    bot.on('message', onMessage);
    bot.once('end', onEnd);
    bot.once('kicked', onKicked);
    bot.once('error', onError);
  });
}

function windowTitle(win) {
  if (!win) {
    return null;
  }
  if (typeof win.title === 'string') {
    return win.title;
  }
  try {
    return JSON.stringify(win.title);
  } catch {
    return String(win.title);
  }
}

function windowSummary(win) {
  return {
    id: win?.id ?? null,
    type: win?.type ?? null,
    title: windowTitle(win),
    slots: win?.slots?.length ?? null
  };
}

function inventorySummary(bot) {
  return bot.inventory.items().map(item => ({
    slot: item.slot,
    name: item.name,
    count: item.count,
    displayName: item.displayName
  }));
}

function countItem(bot, name) {
  return bot.inventory.items().filter(item => item.name === name).reduce((sum, item) => sum + item.count, 0);
}

function findCore(bot) {
  const offhand = bot.inventory.slots[45];
  if (offhand && offhand.name === 'wolf_spawn_egg') {
    return { slot: 45, item: offhand, alreadyOffhand: true };
  }
  const item = bot.inventory.items().find(entry => entry.name === 'wolf_spawn_egg');
  return item ? { slot: item.slot, item, alreadyOffhand: false } : null;
}

async function command(bot, text, waitMs = 1400) {
  log('command', { text });
  bot.chat(text);
  await delay(waitMs);
}

async function openByCommand(bot, text, label) {
  const wait = waitForEvent(bot, 'windowOpen', 8000);
  log('openCommand', { label, text });
  bot.chat(text);
  const [win] = await wait;
  log('windowOpen', { label, window: windowSummary(win) });
  await delay(500);
  return win;
}

async function clickSlot(bot, slot, label, waitMs = 700) {
  const win = bot.currentWindow;
  if (!win) {
    throw new Error(`no current window for ${label}`);
  }
  const item = win.slots?.[slot];
  log('clickSlot', {
    label,
    slot,
    item: item ? { name: item.name, count: item.count, displayName: item.displayName } : null,
    window: windowSummary(win)
  });
  await bot.clickWindow(slot, 0, 0);
  await delay(waitMs);
}

async function clickSlotWaitWindow(bot, slot, label) {
  const wait = waitForEvent(bot, 'windowOpen', 8000);
  await clickSlot(bot, slot, label, 200);
  const [win] = await wait;
  log('windowOpen', { label, window: windowSummary(win) });
  await delay(500);
  return win;
}

async function closeWindow(bot, label) {
  if (!bot.currentWindow) {
    return;
  }
  log('closeWindow', { label, window: windowSummary(bot.currentWindow) });
  bot.closeWindow(bot.currentWindow);
  await delay(500);
}

async function expectPetInfo(bot, expectedStage, expectedDurability, label) {
  const waiter = createChatWaiter(
    bot,
    8000,
    text => text.includes('Runtime-питомец:') || text.includes('Ядро в руке:'),
    label
  );
  await command(bot, '/pet info', 200);
  const result = await waiter;
  log('petInfo', { label, result });
  if (!result.text.includes(`E${expectedStage}`)) {
    throw new Error(`unexpected evolution stage in ${label}: ${result.text}`);
  }
  if (!result.text.includes(`прочность: ${expectedDurability}/7`) && !result.text.includes(`durability: ${expectedDurability}/7`)) {
    throw new Error(`unexpected durability in ${label}: ${result.text}`);
  }
}

async function main() {
  const bot = mineflayer.createBot({ host, port, username, version, auth: 'offline' });
  bot.on('message', (jsonMsg, position) => log('chat', { position, text: jsonMsg.toString() }));
  bot.on('kicked', reason => log('kicked', { reason: String(reason) }));
  bot.on('error', error => log('botError', { message: error.message }));
  bot.on('end', reason => log('end', { reason: String(reason) }));
  bot.on('windowOpen', win => log('windowOpenEvent', { window: windowSummary(win) }));

  await waitForEvent(bot, 'spawn', 90000);
  log('spawn', { gamemode: bot.game?.gameMode ?? null, position: bot.entity?.position?.toString?.() ?? null });
  await delay(1500);

  await command(bot, '/vpc source set');
  await command(bot, '/vpc admin audit SmokeBot');
  await command(bot, '/vpc debugpet');

  const core = findCore(bot);
  if (!core) {
    throw new Error('wolf core not found after relog');
  }
  if (!core.alreadyOffhand) {
    await bot.equip(core.item, 'off-hand');
    await delay(800);
  }
  log('offhandBeforeSummon', {
    slot45: bot.inventory.slots[45] ? { name: bot.inventory.slots[45].name, displayName: bot.inventory.slots[45].displayName } : null
  });

  bot.activateItem(true);
  await delay(4200);
  await command(bot, '/vpc admin audit SmokeBot');
  await command(bot, '/vpc debugpet');
  await expectPetInfo(bot, 1, 6, 'before-evolution');

  await command(bot, `/give ${username} bone 24`);
  await command(bot, `/give ${username} leather 16`);
  await command(bot, `/give ${username} totem_of_undying 1`);
  const beforeBone = countItem(bot, 'bone');
  const beforeLeather = countItem(bot, 'leather');
  const beforeTotem = countItem(bot, 'totem_of_undying');
  log('resourcesBeforeEvolution', { beforeBone, beforeLeather, beforeTotem, items: inventorySummary(bot) });

  await openByCommand(bot, '/pet', 'pet-overview-evolution');
  await clickSlotWaitWindow(bot, 13, 'open-pet-info');
  const evolutionChat = createChatWaiter(
    bot,
    8000,
    text => text.includes('эволюционировал')
      || text.includes('evolved')
      || text.includes('ещё не готов')
      || text.includes('not ready')
      || text.includes('Не повезло')
      || text.includes('Bad luck'),
    'evolution-button'
  );
  await clickSlotWaitWindow(bot, 22, 'evolution-button');
  const evolutionResult = await evolutionChat;
  log('evolutionResult', evolutionResult);
  await closeWindow(bot, 'after-evolution');

  const afterBone = countItem(bot, 'bone');
  const afterLeather = countItem(bot, 'leather');
  log('resourcesAfterEvolution', { afterBone, afterLeather, items: inventorySummary(bot) });
  if (afterBone !== beforeBone - 24) {
    throw new Error(`bone was not consumed as expected: before=${beforeBone} after=${afterBone}`);
  }
  if (afterLeather !== beforeLeather - 16) {
    throw new Error(`leather was not consumed as expected: before=${beforeLeather} after=${afterLeather}`);
  }

  await command(bot, '/vpc debugpet');
  await expectPetInfo(bot, 2, 6, 'after-evolution');

  await openByCommand(bot, '/pet', 'pet-overview-repair');
  const repairChat = createChatWaiter(
    bot,
    8000,
    text => text.includes('Прочность ядра повышена') || text.includes('Core durability increased') || text.includes('Ядро не повреждено'),
    'repair-button'
  );
  await clickSlot(bot, 24, 'repair-button', 1200);
  const repairResult = await repairChat;
  log('repairResult', repairResult);
  await closeWindow(bot, 'after-repair');

  const afterTotem = countItem(bot, 'totem_of_undying');
  log('resourcesAfterRepair', { beforeTotem, afterTotem, items: inventorySummary(bot) });
  if (afterTotem !== beforeTotem - 1) {
    throw new Error(`totem was not consumed as expected: before=${beforeTotem} after=${afterTotem}`);
  }

  await command(bot, '/vpc debugpet');
  await expectPetInfo(bot, 2, 7, 'after-repair');
  await command(bot, '/vpc admin audit SmokeBot');
  await command(bot, '/stop', 1000);
  await delay(1000);
  bot.end('evolution repair smoke complete');
}

main().catch(error => {
  log('fatal', { message: error.message, stack: error.stack });
  process.exitCode = 1;
});
