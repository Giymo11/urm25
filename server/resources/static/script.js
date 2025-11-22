(function () {
  let timerInterval = null;
  let startTime = null;

  const NAMES = [
    "Hannah",
    "Thomas",
    "Alex",
    "Ben",
    "Sophie",
    "Franz",
    "Jakob",
    "Theresa",
    "Anna",
    "Chris",
  ];

  const HOBBY_DURATION = [
    "10m",
    "2h",
    "30m",
    "41m",
    "1h",
    "45m",
    "50m",
    "23m",
    "3h",
    "37m",
  ];

  const getUserIdFromPath = () => {
    const parts = window.location.pathname.split("/").filter(Boolean); // expect /p/:userId
    return parts.length >= 2 && parts[0] === "p"
      ? decodeURIComponent(parts[1])
      : null;
  };

  const stateUrl = (userId) => `/api/${encodeURIComponent(userId)}/state`;
  const startUrl = (userId) =>
    `/api/${encodeURIComponent(userId)}/start_tracking`;
  const stopUrl = (userId) =>
    `/api/${encodeURIComponent(userId)}/stop_tracking`;

  const fetchState = async (userId) => {
    const res = await fetch(stateUrl(userId), { cache: "no-store" });
    if (!res.ok) throw new Error(`State request failed: ${res.status}`);
    return res.json();
  };

  const hourSeededFriends = () => {
    const hour = new Date().getHours();
    const seeded = Math.abs(Math.sin(hour * 37)) * 9; // 0-9
    return 1 + Math.floor(seeded); // 1-10
  };

  const renderCondition = (condition, userId) => {
    const cond = (condition || "").toUpperCase();
    const scenarioA = document.querySelector(".scenario-a");
    const scenarioB = document.querySelector(".scenario-b");
    const mode = document.querySelector(".mode");
    const friendsNo = document.querySelector(".friends-no");
    const name1 = document.querySelector(".name1");
    const name2 = document.querySelector(".name2");
    const duration1 = document.querySelector(".duration1");
    const duration2 = document.querySelector(".duration2");
    const time1 = document.querySelector(".time1");
    const time2 = document.querySelector(".time2");
    if (cond === "A") {
      scenarioA.style.display = "flex";
      scenarioB.style.display = "none";
      mode.textContent = "Private Mode";
    } else if (cond === "B") {
      scenarioA.style.display = "none";
      scenarioB.style.display = "flex";
      mode.textContent = "Social Mode";
      const hourSeed = hourSeededFriends();
      friendsNo.textContent = hourSeed;
      name1.textContent = NAMES.at(hourSeed - 1);
      name2.textContent = NAMES.at(hourSeed - 2);
      duration1.textContent = HOBBY_DURATION.at(hourSeed % 10);
      duration2.textContent = HOBBY_DURATION.at((hourSeed % 10) - 3);
      time1.textContent = HOBBY_DURATION.at((hourSeed % 10) - 1);
      time2.textContent = HOBBY_DURATION.at((hourSeed % 10) - 2);
    } else {
      scenarioA.style.display = "none";
      scenarioB.style.display = "none";
      mode.textContent = "We've encountered an issue";
    }
  };

  const renderTrackingControls = (condition, state, userId) => {
    const toggleButton = document.querySelector(".bottom-bar button");
    const timerPanels = document.querySelectorAll(".timer-panel");
    // Use global startTime and timerInterval
    if (timerInterval) {
      clearInterval(timerInterval);
      timerInterval = null;
    }
    startTime = state.tracking_timestamp
      ? new Date(state.tracking_timestamp).getTime()
      : null;

    function formatTime(secs) {
      const m = String(Math.floor(secs / 60)).padStart(2, "0");
      const s = String(secs % 60).padStart(2, "0");
      return `${m}:${s}`;
    }

    function updateTimers() {
      if (!startTime) return;
      const elapsed = Math.floor((Date.now() - startTime) / 1000);
      timerPanels.forEach((panel) => {
        panel.textContent = formatTime(elapsed);
      });
    }

    const isTracking = !!state.tracking_timestamp;
    // Show/hide timer panels based on tracking state
    timerPanels.forEach((panel) => {
      panel.style.display = isTracking ? "" : "none";
      // Optionally hide the parent .timer as well:
      if (
        panel.parentElement &&
        panel.parentElement.classList.contains("timer")
      ) {
        panel.parentElement.style.display = isTracking ? "" : "none";
      }
    });

    if (isTracking) {
      toggleButton.innerHTML = `<span class="material-symbols-outlined" style="font-size: 30px">stop</span>Stop tracking&nbsp;`;
      toggleButton.classList.remove("start");
      toggleButton.classList.add("stop");
      updateTimers();
      timerInterval = setInterval(updateTimers, 1000);
    } else {
      toggleButton.innerHTML = `<span class="material-symbols-outlined" style="font-size: 30px">play_arrow</span>Start tracking&nbsp;`;
      toggleButton.classList.remove("stop");
      toggleButton.classList.add("start");
      timerPanels.forEach((panel) => (panel.textContent = "00:00"));
      startTime = null;
    }

    toggleButton.onclick = async () => {
      try {
        toggleButton.disabled = true;
        const url = isTracking ? stopUrl(userId) : startUrl(userId);
        const res = await fetch(url, { method: "POST" });
        if (!res.ok) throw new Error("Failed to update tracking");
        const next = await res.json();
        const nextCond = next.current_condition || condition;
        renderTrackingControls(nextCond, next, userId);
      } catch (err) {
        console.error(err);
      } finally {
        toggleButton.disabled = false;
      }
    };
  };

  (async function init() {
    const userId = getUserIdFromPath();
    if (!userId) {
      const startButton = document.querySelector(".bottom-bar button");
      if (startButton) startButton.disabled = true;
      return;
    }

    try {
      const state = await fetchState(userId);
      const condition = state.current_condition || state.condition;
      renderCondition(condition, userId);
      renderTrackingControls(condition, state, userId);
    } catch (err) {
      console.error(err);
      const startButton = document.querySelector(".bottom-bar button");
      if (startButton) startButton.disabled = true;
    }
  })();
})();
