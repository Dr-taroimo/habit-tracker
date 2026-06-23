const STORAGE_KEY = "habit-tracker-web:v1";
const NOTICE_TIMEOUT_MS = 3200;

const elements = {
  form: document.querySelector("#habit-form"),
  input: document.querySelector("#habit-input"),
  habits: document.querySelector("#habits"),
  template: document.querySelector("#habit-template"),
  clearButton: document.querySelector("#clear-button"),
  soundToggle: document.querySelector("#sound-toggle"),
  hour: document.querySelector("#notify-hour"),
  minute: document.querySelector("#notify-minute"),
  permissionButton: document.querySelector("#permission-button"),
  saveTimeButton: document.querySelector("#save-time-button"),
  notice: document.querySelector("#notice"),
};

const state = loadState();
let audioContext = null;
let noticeTimer = null;
let lastNotificationDate = "";

initialize();

function initialize() {
  elements.soundToggle.checked = state.soundEnabled;
  elements.hour.value = String(state.notification.hour).padStart(2, "0");
  elements.minute.value = String(state.notification.minute).padStart(2, "0");

  elements.form.addEventListener("submit", handleAddHabit);
  elements.clearButton.addEventListener("click", clearHabits);
  elements.soundToggle.addEventListener("change", () => {
    state.soundEnabled = elements.soundToggle.checked;
    saveState();
  });
  elements.permissionButton.addEventListener("click", requestNotificationPermission);
  elements.saveTimeButton.addEventListener("click", saveNotificationTime);
  window.setInterval(checkScheduledNotification, 30 * 1000);

  renderHabits();
  checkScheduledNotification();
}

function handleAddHabit(event) {
  event.preventDefault();
  const name = elements.input.value.trim();
  if (!name) {
    showNotice("習慣名を入力してください");
    return;
  }

  state.habits.unshift({
    id: crypto.randomUUID ? crypto.randomUUID() : String(Date.now()),
    name,
    completedDates: [],
  });
  elements.input.value = "";
  saveState();
  playSound("add");
  renderHabits();
  showNotice("習慣を追加しました");
}

function clearHabits() {
  if (state.habits.length === 0) {
    showNotice("削除する習慣はありません");
    return;
  }

  state.habits = [];
  saveState();
  renderHabits();
  showNotice("習慣をすべて削除しました");
}

function toggleToday(habitId) {
  const habit = state.habits.find((item) => item.id === habitId);
  if (!habit) return;

  const today = dateKey(new Date());
  const completed = habit.completedDates.includes(today);
  if (completed) {
    habit.completedDates = habit.completedDates.filter((date) => date !== today);
    playSound("undo");
  } else {
    habit.completedDates.push(today);
    habit.completedDates = [...new Set(habit.completedDates)].sort();
    playSound("complete");
  }

  saveState();
  renderHabits();
}

function renderHabits() {
  elements.habits.replaceChildren();

  if (state.habits.length === 0) {
    const empty = document.createElement("article");
    empty.className = "empty-state";
    empty.innerHTML = `
      <h3>まだ習慣がありません</h3>
      <p>まずはひとつ、続けたいことを書いてみましょう。</p>
    `;
    elements.habits.append(empty);
    return;
  }

  state.habits.forEach((habit) => {
    const today = dateKey(new Date());
    const completeToday = habit.completedDates.includes(today);
    const fragment = elements.template.content.cloneNode(true);
    const card = fragment.querySelector(".habit-card");
    const title = fragment.querySelector("h3");
    const status = fragment.querySelector(".habit-card__state");
    const pills = fragment.querySelectorAll(".pill");
    const weekDots = fragment.querySelector(".week-dots");
    const button = fragment.querySelector(".habit-card__button");

    card.classList.toggle("is-complete", completeToday);
    title.textContent = habit.name;
    status.textContent = completeToday ? "今日もできました" : "今日はまだです";
    pills[0].textContent = `連続${streakDays(habit)}日`;
    pills[1].textContent = `今週${weekCompletedCount(habit)}/7`;
    button.textContent = completeToday ? "取り消す" : "今日できた";
    button.classList.toggle("button--soft", completeToday);
    button.classList.toggle("button--coral", !completeToday);
    button.addEventListener("click", () => toggleToday(habit.id));

    weekDates().forEach(({ key, label }) => {
      const item = document.createElement("div");
      item.className = "day-dot";
      item.classList.toggle("is-complete", habit.completedDates.includes(key));
      item.innerHTML = `<span></span><small>${label}</small>`;
      weekDots.append(item);
    });

    elements.habits.append(fragment);
  });
}

async function requestNotificationPermission() {
  if (!("Notification" in window)) {
    showNotice("このブラウザでは通知を利用できません");
    return;
  }

  const result = await Notification.requestPermission();
  if (result === "granted") {
    showNotice("通知を許可しました");
  } else {
    showNotice("通知を使うには許可が必要です");
  }
}

function saveNotificationTime() {
  const hour = Number.parseInt(elements.hour.value.trim(), 10);
  const minute = Number.parseInt(elements.minute.value.trim(), 10);

  if (!Number.isInteger(hour) || !Number.isInteger(minute) || hour < 0 || hour > 23 || minute < 0 || minute > 59) {
    showNotice("通知時刻を正しく入力してください");
    return;
  }

  state.notification = { hour, minute };
  elements.hour.value = String(hour).padStart(2, "0");
  elements.minute.value = String(minute).padStart(2, "0");
  saveState();
  showNotice("毎日の通知を設定しました");
  sendNotification("毎日の通知を設定しました", "ページを開いている間、設定時刻にお知らせします。");
}

function checkScheduledNotification() {
  const now = new Date();
  const today = dateKey(now);
  if (lastNotificationDate === today) return;
  if (now.getHours() !== state.notification.hour || now.getMinutes() !== state.notification.minute) return;

  lastNotificationDate = today;
  showNotice("今日の習慣をチェックしましょう");
  sendNotification("習慣トラッカー", "今日の習慣をチェックしましょう");
}

function sendNotification(title, body) {
  if (!("Notification" in window) || Notification.permission !== "granted") return;
  new Notification(title, { body });
}

function streakDays(habit) {
  let streak = 0;
  let cursor = startOfToday();
  while (habit.completedDates.includes(dateKey(cursor))) {
    streak += 1;
    cursor.setDate(cursor.getDate() - 1);
  }
  return streak;
}

function weekCompletedCount(habit) {
  return weekDates().filter(({ key }) => habit.completedDates.includes(key)).length;
}

function weekDates() {
  return Array.from({ length: 7 }, (_, index) => {
    const date = startOfToday();
    date.setDate(date.getDate() - (6 - index));
    return {
      key: dateKey(date),
      label: ["日", "月", "火", "水", "木", "金", "土"][date.getDay()],
    };
  });
}

function startOfToday() {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  return date;
}

function dateKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function showNotice(message) {
  elements.notice.textContent = message;
  window.clearTimeout(noticeTimer);
  noticeTimer = window.setTimeout(() => {
    elements.notice.textContent = "";
  }, NOTICE_TIMEOUT_MS);
}

function playSound(type) {
  if (!state.soundEnabled) return;
  audioContext = audioContext || new AudioContext();

  const patterns = {
    add: [523.25, 659.25],
    complete: [659.25, 783.99, 1046.5],
    undo: [392, 329.63],
  };
  const frequencies = patterns[type] || patterns.add;
  const now = audioContext.currentTime;

  frequencies.forEach((frequency, index) => {
    const oscillator = audioContext.createOscillator();
    const gain = audioContext.createGain();
    const start = now + index * 0.08;
    const end = start + 0.13;

    oscillator.type = "sine";
    oscillator.frequency.value = frequency;
    gain.gain.setValueAtTime(0.0001, start);
    gain.gain.exponentialRampToValueAtTime(0.18, start + 0.018);
    gain.gain.exponentialRampToValueAtTime(0.0001, end);
    oscillator.connect(gain).connect(audioContext.destination);
    oscillator.start(start);
    oscillator.stop(end + 0.02);
  });
}

function loadState() {
  const defaults = {
    habits: [],
    soundEnabled: true,
    notification: { hour: 20, minute: 0 },
  };

  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
    return {
      ...defaults,
      ...parsed,
      habits: Array.isArray(parsed?.habits) ? parsed.habits : [],
      notification: {
        ...defaults.notification,
        ...(parsed?.notification || {}),
      },
    };
  } catch {
    return defaults;
  }
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}
