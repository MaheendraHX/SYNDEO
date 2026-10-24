
(() => {
  const MAX_PHOTO_BYTES = 1024 * 1024;
  const message = document.querySelector("#app-message");

  const taskStatuses = [
    { value: "to-do", label: "To do" },
    { value: "in-progress", label: "In progress" },
    { value: "done", label: "Done" }
  ];

  /*
   * Convert the availability labels used in the frontend into the
   * codes stored by the backend.
   *
   * Supports either UI values such as "Weekends" or backend values
   * such as "WEEKENDS".
   */
  const availabilityCodes = {
    FLEXIBLE: "FLEXIBLE",
    WEEKDAYS: "WEEKDAYS",
    EVENINGS: "EVENINGS",
    WEEKENDS: "WEEKENDS",
    FLEXIBLE_LABEL: "FLEXIBLE",
    "WEEK DAYS": "WEEKDAYS",
    "WEEKDAYS": "WEEKDAYS",
    "EVENINGS": "EVENINGS",
    "WEEKENDS": "WEEKENDS"
  };

  function normalizeAvailabilityCode(value) {
    const normalized = String(value || "")
      .trim()
      .toUpperCase()
      .replace(/[_-]+/g, " ")
      .replace(/\s+/g, " ");

    if (normalized === "FLEXIBLE") return "FLEXIBLE";
    if (normalized === "WEEKDAYS" || normalized === "WEEK DAYS") return "WEEKDAYS";
    if (normalized === "EVENINGS") return "EVENINGS";
    if (normalized === "WEEKENDS") return "WEEKENDS";

    return normalized;
  }

  function announce(text, state) {
    if (!message) return;

    message.textContent = text;

    if (state) {
      message.dataset.state = state;
    } else {
      delete message.dataset.state;
    }
  }

  function photoStorageKey(userId) {
    return `syndeo.profile.photo.v1.${userId}`;
  }

  function readLocalPhoto(userId) {
    try {
      return window.localStorage.getItem(photoStorageKey(userId)) || "";
    } catch (error) {
      console.error("Could not read the locally stored profile photo.", error);
      announce("This browser could not load the saved profile photo.", "error");
      return "";
    }
  }

  function writeLocalPhoto(userId, photo) {
    try {
      const key = photoStorageKey(userId);
      if (photo) {
        window.localStorage.setItem(key, photo);
      } else {
        window.localStorage.removeItem(key);
      }
      return true;
    } catch (error) {
      console.error("Could not save the profile photo in this browser.", error);
      announce("The browser could not save your profile photo.", "error");
      return false;
    }
  }

  function makeElement(tag, className, text) {
    const element = document.createElement(tag);

    if (className) element.className = className;
    if (text !== undefined) element.textContent = text;

    return element;
  }

  function initialsFor(name) {
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toLocaleUpperCase())
      .join("") || "?";
  }

  function renderAvatar(container, name, image, small) {
    container.replaceChildren();
    container.classList.toggle("profile-avatar-small", Boolean(small));

    if (image) {
      const img = document.createElement("img");
      img.src = image;
      img.alt = "";
      container.append(img);
      container.setAttribute("aria-label", `${name || "Profile"} picture`);
      return;
    }

    container.textContent = initialsFor(name || "");
    container.setAttribute("aria-label", `${name || "Profile"} initials`);
  }

  function renderTags(container, values, emptyText) {
    container.replaceChildren();

    if (!values.length) {
      container.append(makeElement("span", "app-help", emptyText));
      return;
    }

    values.forEach((value) => {
      container.append(makeElement("span", "tag-chip", value));
    });
  }

  function addChip(list, value) {
    const cleanValue = value.trim().replace(/\s+/g, " ");

    if (!cleanValue) return false;

    if (
      list.some(
        (entry) =>
          entry.toLocaleLowerCase() === cleanValue.toLocaleLowerCase()
      )
    ) {
      announce(`“${cleanValue}” is already in this list.`);
      return false;
    }

    list.push(cleanValue);
    return true;
  }

  function renderEditableChips(values, container, label, onRemove) {
    container.replaceChildren();

    if (!values.length) {
      container.append(
        makeElement("span", "app-help", `No ${label.toLowerCase()} added yet.`)
      );
      return;
    }

    values.forEach((value, index) => {
      const chip = makeElement("span", "tag-chip");
      chip.append(document.createTextNode(value));

      const remove = makeElement("button", "tag-chip-remove", "×");
      remove.type = "button";
      remove.setAttribute(
        "aria-label",
        `Remove ${value} ${label.toLowerCase()}`
      );

      remove.addEventListener("click", () => onRemove(index));

      chip.append(remove);
      container.append(chip);
    });
  }

  // ============================================================
  // PROFILE
  // ============================================================

  function setupProfile() {
    const form = document.querySelector("#profile-form");
    if (!form) return;

    const fields = {
      name: document.querySelector("#profile-name"),
      bio: document.querySelector("#profile-bio"),
      experience: document.querySelector("#experience"),
      goal: document.querySelector("#profile-goal"),
      photo: document.querySelector("#profile-photo"),
      skillEntry: document.querySelector("#skill-entry"),
      interestEntry: document.querySelector("#interest-entry"),
      skills: document.querySelector("#skill-list"),
      interests: document.querySelector("#interest-list"),
      avatar: document.querySelector("#profile-avatar"),
      removePhoto: document.querySelector("#remove-photo")
    };

    const state = {
      skills: [],
      interests: [],
      availability: [],
      photo: ""
    };

    let serverProfileLoaded = false;
    let accountUserId = null;

    function setAvailabilityInputs(values) {
      const selectedCodes = new Set(
        values.map(normalizeAvailabilityCode)
      );

      form
        .querySelectorAll('input[name="availability"]')
        .forEach((input) => {
          input.checked = selectedCodes.has(
            normalizeAvailabilityCode(input.value)
          );
        });
    }

    function getSelectedAvailabilityLabels() {
      return [
        ...form.querySelectorAll('input[name="availability"]:checked')
      ].map((input) => input.value);
    }

    function getSelectedAvailabilityCodes() {
      return getSelectedAvailabilityLabels().map(
        normalizeAvailabilityCode
      );
    }

    setAvailabilityInputs(state.availability);

    function renderPreview() {
      const name = fields.name.value.trim();

      const previewTitle = document.querySelector("#profile-preview-title");
      const previewExperience = document.querySelector("#preview-experience");
      const previewBio = document.querySelector("#preview-bio");
      const previewGoal = document.querySelector("#preview-goal");
      const previewSkills = document.querySelector("#preview-skills");
      const previewInterests = document.querySelector("#preview-interests");
      const previewAvailability = document.querySelector("#preview-availability");

      if (previewTitle) {
        previewTitle.textContent = name || "Your profile";
      }

      if (previewExperience) {
        previewExperience.textContent =
          fields.experience.value || "Add details to introduce yourself.";
      }

      if (previewBio) {
        previewBio.textContent =
          fields.bio.value.trim() || "Your introduction will appear here.";
      }

      if (previewGoal) {
        previewGoal.textContent =
          fields.goal.value.trim() || "Add a goal when you are ready.";
      }

      if (previewSkills) {
        renderTags(
          previewSkills,
          state.skills,
          "Add a skill to introduce what you bring."
        );
      }

      if (previewInterests) {
        renderTags(
          previewInterests,
          state.interests,
          "Add interests to show what you care about."
        );
      }

      if (previewAvailability) {
        renderTags(
          previewAvailability,
          getSelectedAvailabilityLabels(),
          "Choose when you might collaborate."
        );
      }

      renderAvatar(fields.avatar, name, state.photo, false);
      fields.removePhoto.hidden = !state.photo;

      renderEditableChips(
        state.skills,
        fields.skills,
        "Skills",
        (index) => {
          state.skills.splice(index, 1);
          renderPreview();
        }
      );

      renderEditableChips(
        state.interests,
        fields.interests,
        "Interests",
        (index) => {
          state.interests.splice(index, 1);
          renderPreview();
        }
      );
    }

    function addEntry(input, values, label) {
      if (!addChip(values, input.value)) return;

      input.value = "";
      renderPreview();
      announce(`${label} added.`);
    }

    document.querySelector("#add-skill").addEventListener("click", () => {
      addEntry(fields.skillEntry, state.skills, "Skill");
    });

    document.querySelector("#add-interest").addEventListener("click", () => {
      addEntry(fields.interestEntry, state.interests, "Interest");
    });

    [
      [fields.skillEntry, "#add-skill"],
      [fields.interestEntry, "#add-interest"]
    ].forEach(([input, buttonSelector]) => {
      input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === ",") {
          event.preventDefault();
          document.querySelector(buttonSelector).click();
        }
      });
    });

    [
      fields.name,
      fields.bio,
      fields.experience,
      fields.goal
    ].forEach((input) => {
      input.addEventListener("input", renderPreview);
      input.addEventListener("change", renderPreview);
    });

    form
      .querySelectorAll('input[name="availability"]')
      .forEach((input) => {
        input.addEventListener("change", renderPreview);
      });

    fields.photo.addEventListener("change", () => {
      const file = fields.photo.files && fields.photo.files[0];
      if (!file) return;

      if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
        fields.photo.value = "";
        announce("Choose a JPG, PNG, or WebP image.", "error");
        return;
      }

      if (file.size > MAX_PHOTO_BYTES) {
        fields.photo.value = "";
        announce("That image is over 1 MB. Choose a smaller picture.", "error");
        return;
      }

      const reader = new FileReader();

      reader.addEventListener("load", () => {
        if (typeof reader.result !== "string") {
          announce("The selected image could not be previewed.", "error");
          return;
        }

        state.photo = reader.result;
        renderPreview();

        announce(
          "Picture preview updated. Save your profile to keep it in this browser.",
          "success"
        );
      });

      reader.addEventListener("error", () => {
        console.error(
          "Could not read the selected profile picture.",
          reader.error
        );

        announce(
          "The selected picture could not be read. Please try another image.",
          "error"
        );
      });

      reader.readAsDataURL(file);
    });

    fields.removePhoto.addEventListener("click", () => {
      state.photo = "";
      fields.photo.value = "";
      renderPreview();

      announce(
        "Picture removed from the preview. Save your profile to keep this change.",
        "success"
      );
    });

    // Save the profile.
    form.addEventListener("submit", async (event) => {
      event.preventDefault();

      const isSignedIn = Boolean(window.SyndeoAPI?.getToken());

      if (isSignedIn && !serverProfileLoaded) {
        announce(
          "Your account has not loaded successfully. Please reload the profile and try again.",
          "error"
        );
        return;
      }

      fields.skillEntry.setCustomValidity(
        state.skills.length
          ? ""
          : "Add at least one skill to your profile."
      );

      if (!form.reportValidity()) return;

      const profile = {
        name: fields.name.value.trim(),
        bio: fields.bio.value.trim(),
        skills: [...state.skills],
        interests: [...state.interests],
        experience: fields.experience.value,
        goal: fields.goal.value.trim(),
        availability: getSelectedAvailabilityLabels(),
        photo: state.photo
      };

      if (!isSignedIn) {
        showLoginPrompt(document.querySelector(".profile-layout"));
        return;
      }

      try {
        const updatedAccount = await window.SyndeoAPI.updateMyProfile({
          displayName: profile.name,
          bio: profile.bio || null,
          experienceLevel: profile.experience || null,
          goals: profile.goal || null,
          skills: profile.skills,
          interests: profile.interests,
          availability: getSelectedAvailabilityCodes()
        });

        // The server is the source of truth for account profile details.
        // The profile photo remains browser-local.
        if (updatedAccount) {
          profile.name = updatedAccount.displayName || profile.name;
          profile.bio = updatedAccount.bio || "";
          profile.experience =
            updatedAccount.experienceLevel || "";
          profile.goal = updatedAccount.goals || "";

          profile.skills = Array.isArray(updatedAccount.skills)
            ? updatedAccount.skills
            : profile.skills;

          profile.interests = Array.isArray(updatedAccount.interests)
            ? updatedAccount.interests
            : profile.interests;

          profile.availability = Array.isArray(updatedAccount.availability)
            ? updatedAccount.availability.map((code) => {
                const matchingInput = [
                  ...form.querySelectorAll('input[name="availability"]')
                ].find(
                  (input) =>
                    normalizeAvailabilityCode(input.value) ===
                    normalizeAvailabilityCode(code)
                );

                return matchingInput ? matchingInput.value : code;
              })
            : profile.availability;
        }

        const photoSaved = writeLocalPhoto(accountUserId, state.photo);

        if (photoSaved) {
          announce(
            "Profile details saved to your account. The profile photo is stored only in this browser.",
            "success"
          );
        } else {
          announce(
            "Profile details were saved to your account, but the browser could not save the photo.",
            "error"
          );
        }
      } catch (error) {
        console.error("Could not save the SYNDEO profile.", error);

        announce(
          error?.message ||
            "Your profile could not be saved. Please try again.",
          "error"
        );
      }
    });

    fields.skillEntry.addEventListener("input", () => {
      fields.skillEntry.setCustomValidity("");
    });

    renderPreview();

    if (!window.SyndeoAPI?.getToken()) {
      showLoginPrompt(document.querySelector(".profile-layout"));
      form.querySelectorAll("input, textarea, select, button").forEach((control) => {
        control.disabled = true;
      });
      return;
    }

    (async () => {
      try {
        const account = await window.SyndeoAPI.getMyProfile();

        accountUserId = account.userId;
        fields.name.value = account.displayName || "";
        fields.bio.value = account.bio || "";
        fields.experience.value = account.experienceLevel || "";
        fields.goal.value = account.goals || "";
        state.skills = Array.isArray(account.skills) ? account.skills : [];
        state.interests = Array.isArray(account.interests) ? account.interests : [];
        state.availability = Array.isArray(account.availability)
          ? account.availability
          : [];
        state.photo = readLocalPhoto(accountUserId);
        setAvailabilityInputs(state.availability);
        serverProfileLoaded = true;
        renderPreview();
      } catch (error) {
        console.error("Could not load the SYNDEO profile.", error);
        serverProfileLoaded = false;
        handleFeatureError(error, document.querySelector(".profile-layout"));
      }
    })();
  }

  // ============================================================
  // ACCOUNT IDENTITY
  // ============================================================

  async function setupAccountIdentity() {
    const nameElement = document.querySelector("#account-identity-name");
    const emailElement = document.querySelector("#account-identity-email");

    // This section only exists on the profile page.
    if (!nameElement || !emailElement) return;

    if (!window.SyndeoAPI?.getToken()) {
      nameElement.textContent = "You're not signed in.";
      emailElement.textContent = "Sign in to load your account details.";
      return;
    }

    nameElement.textContent = "Loading account…";
    emailElement.textContent = "";

    try {
      const account = await window.SyndeoAPI.getMyProfile();

      nameElement.textContent =
        account.displayName || "Name not provided";

      emailElement.textContent =
        account.email || "Email not provided";
    } catch (error) {
      nameElement.textContent = "Account details unavailable.";

      emailElement.textContent =
        error?.message || "Please sign in again and retry.";
    }
  }

  // ============================================================
  // SHARED PROFILE TAGS
  // ============================================================

  function appendProfileTags(parent, heading, values) {
    parent.append(makeElement("h3", "", heading));

    const tags = makeElement("div", "tag-list");
    renderTags(tags, values, "Not listed");

    parent.append(tags);
  }

  function showLoginPrompt(container, rejected = false) {
    const empty = makeElement("div", "empty-state");
    empty.append(
      makeElement("h2", "", "Sign in to continue."),
      makeElement(
        "p",
        "",
        rejected
          ? "The server rejected this sign-in token. Please sign in again."
          : "Your session is missing."
      )
    );
    const login = makeElement("a", "button", "Go to sign in");
    login.href = "login.html";
    login.dataset.pageTransition = "";
    empty.append(login);
    container.replaceChildren(empty);
    announce("Sign in to use SYNDEO account features.", "error");
  }

  function handleFeatureError(error, container) {
    if (error?.status === 401 || !window.SyndeoAPI?.getToken()) {
      showLoginPrompt(container, error?.status === 401);
      return;
    }
    const message = error?.message || "The request could not be completed.";
    container.querySelector(".feature-error")?.remove();
    const errorState = makeElement("p", "app-message feature-error", message);
    errorState.setAttribute("role", "alert");
    container.append(errorState);
    announce(message, "error");
  }

  // ============================================================
  // DISCOVERY
  // ============================================================

  function setupDiscovery() {
    const list = document.querySelector("#collaborator-list");
    if (!list) return;

    const skillFilter = document.querySelector("#skill-filter");
    const interestFilter = document.querySelector("#interest-filter");
    const search = document.querySelector("#collaborator-search");
    const dialog = document.querySelector("#detail-dialog");
    const detailBody = document.querySelector("#detail-body");
    const resultCount = document.querySelector("#result-count");
    let profiles = [];
    let requestNumber = 0;
    let searchTimer;

    function openPerson(person) {
      detailBody.replaceChildren();

      const heading = makeElement("h2", "", person.name);
      heading.id = "detail-title";

      detailBody.append(heading, makeElement("p", "", person.bio || "No bio provided."));

      appendProfileTags(detailBody, "Skills they bring", person.skills);
      appendProfileTags(detailBody, "Interests", person.interests);

      detailBody.append(
        makeElement("h3", "", "Experience"),
        makeElement("p", "", person.experience || "Not listed"),
        makeElement("h3", "", "Looking to"),
        makeElement("p", "", person.goal || "Not listed"),
        makeElement("h3", "", "Availability"),
        makeElement(
          "p",
          "",
          person.availability.length
            ? person.availability.join(", ")
            : "Not listed"
        ),
        makeElement(
          "p",
          "fit-note",
          "Use these profile details to decide whether your skills, interests, and availability could work well together."
        )
      );

      const circleLink = makeElement("a", "button", "Explore circles");
      circleLink.href = "circles.html";
      circleLink.dataset.pageTransition = "";

      detailBody.append(circleLink);

      if (typeof dialog.showModal === "function") {
        dialog.showModal();
      }
    }

    function renderPeople() {
      list.replaceChildren();
      resultCount.textContent =
        `${profiles.length} ${profiles.length === 1 ? "profile" : "profiles"}`;
      if (!profiles.length) {
        const empty = makeElement("div", "empty-state");
        empty.append(
          makeElement("h2", "", "No profiles match those filters."),
          makeElement(
            "p",
            "",
            "Try another search or clear one of the filters."
          )
        );
        list.append(empty);
        return;
      }

      profiles.forEach((person) => {
        const card = makeElement("article", "collaborator-card");
        const initials = person.name
          .split(/\s+/)
          .filter(Boolean)
          .slice(0, 2)
          .map((part) => part[0])
          .join("")
          .toUpperCase();
        const avatar = makeElement("div", "person-avatar", initials);
        avatar.setAttribute("aria-hidden", "true");
        const main = makeElement("div", "collaborator-main");
        main.append(
          makeElement("h2", "", person.name),
          makeElement(
            "p",
            "collaborator-meta",
            [
              person.experience,
              person.availability.length
                ? person.availability.join(", ")
                : "Availability not listed"
            ].filter(Boolean).join(" · ")
          ),
          makeElement("p", "", person.bio || "No bio provided.")
        );
        main.append(
          makeElement("h3", "card-section-label", "Skills they bring")
        );
        const skillTags = makeElement("div", "card-tags");
        person.skills.forEach((item) => {
          skillTags.append(makeElement("span", "tag-chip", item));
        });
        const interests = makeElement(
          "p",
          "app-help",
          person.interests.length
            ? `Interested in ${person.interests.join(" · ")}`
            : "No interests listed."
        );
        main.append(skillTags, interests);
        const actions = makeElement("div", "collaborator-actions");
        actions.append(
          makeElement(
            "h3",
            "card-section-label",
            "Collaboration details"
          )
        );
        const detailButton = makeElement(
          "button",
          "button button-subtle",
          "View profile"
        );

        detailButton.type = "button";
        detailButton.addEventListener("click", () => openPerson(person));

        const circleLink = makeElement("a", "button", "Explore circles");
        circleLink.href = "circles.html";
        circleLink.dataset.pageTransition = "";
        actions.append(
          makeElement(
            "p",
            "fit-note",
            person.goal
              ? `Looking to: ${person.goal}`
              : "Review this profile's skills, interests, and availability."
          ),
          detailButton,
          circleLink
        );
        card.append(avatar, main, actions);
        list.append(card);
      });
    }

    function populateFilters(items) {
      const currentSkill = skillFilter.value;
      const currentInterest = interestFilter.value;
      const skills = [...new Set(items.flatMap((person) => person.skills || []))].sort();
      const interests = [...new Set(items.flatMap((person) => person.interests || []))].sort();
      skillFilter.replaceChildren(new Option("Any skill", ""));
      interestFilter.replaceChildren(new Option("Any interest", ""));
      skills.forEach((skill) => skillFilter.add(new Option(skill, skill)));
      interests.forEach((interest) => interestFilter.add(new Option(interest, interest)));
      skillFilter.value = skills.includes(currentSkill) ? currentSkill : "";
      interestFilter.value = interests.includes(currentInterest) ? currentInterest : "";
    }

    async function loadProfiles(populateOptions = false) {
      if (!window.SyndeoAPI?.getToken()) {
        showLoginPrompt(list);
        return;
      }
      const currentRequest = ++requestNumber;
      list.replaceChildren(makeElement("p", "app-help", "Loading profiles…"));
      resultCount.textContent = "";
      try {
        const data = await window.SyndeoAPI.getDiscoveryProfiles({
          search: search.value.trim(),
          skill: skillFilter.value,
          interest: interestFilter.value
        });
        if (currentRequest !== requestNumber) return;
        profiles = Array.isArray(data) ? data : [];
        profiles = profiles.map((person) => ({
          ...person,
          skills: Array.isArray(person.skills) ? person.skills : [],
          interests: Array.isArray(person.interests) ? person.interests : [],
          availability: Array.isArray(person.availability) ? person.availability : []
        }));
        if (populateOptions) populateFilters(profiles);
        renderPeople();
        announce("", "");
      } catch (error) {
        if (currentRequest !== requestNumber) return;
        list.replaceChildren();
        resultCount.textContent = "";
        handleFeatureError(error, list);
      }
    }

    search.addEventListener("input", () => {
      clearTimeout(searchTimer);
      searchTimer = setTimeout(() => loadProfiles(), 250);
    });
    skillFilter.addEventListener("change", () => loadProfiles());
    interestFilter.addEventListener("change", () => loadProfiles());

    document
      .querySelectorAll("[data-close-dialog]")
      .forEach((button) => {
        button.addEventListener("click", () => dialog.close());
      });

    dialog.addEventListener("click", (event) => {
      if (event.target === dialog) dialog.close();
    });

    loadProfiles(true);
  }

  // ============================================================
  // CIRCLES
  // ============================================================

  function setupCircles() {
    const list = document.querySelector("#circle-list");
    if (!list) return;

    const dialog = document.querySelector("#detail-dialog");
    const detailBody = document.querySelector("#detail-body");
    const createPanel = document.querySelector("#create-circle-panel");
    const toggleButton = document.querySelector("#toggle-create-circle");
    const circleForm = document.querySelector("#circle-form");
    const circleCount = document.querySelector("#circle-count");
    let circles = [];
    let busy = false;

    function showDetails(circle) {
      detailBody.replaceChildren();
      const heading = makeElement("h2", "", circle.name);
      heading.id = "detail-title";
      detailBody.append(
        makeElement("span", "circle-type", circle.purpose),
        heading,
        makeElement("p", "", circle.description),
        makeElement("h3", "", "Group size"),
        makeElement(
          "p",
          "",
          `${circle.memberCount} ${circle.memberCount === 1 ? "person" : "people"}`
        ),
        makeElement("p", "app-help", "Circle tags are not stored by the current server schema.")
      );
      if (Array.isArray(circle.members) && circle.members.length) {
        detailBody.append(makeElement("h3", "", "Members"));
        const members = makeElement("ul", "");
        circle.members.forEach((member) => {
          members.append(makeElement(
            "li",
            "",
            `${member.displayName} · ${member.role.toLowerCase()}`
          ));
        });
        detailBody.append(members);
      }
      if (typeof dialog.showModal === "function") dialog.showModal();
    }

    function renderCircles() {
      if (!window.SyndeoAPI?.getToken()) {
        showLoginPrompt(list);
        return;
      }
      list.replaceChildren();
      circleCount.textContent =
        `${circles.length} ${circles.length === 1 ? "circle" : "circles"}`;
      if (!circles.length) {
        const empty = makeElement("div", "empty-state");
        empty.append(
          makeElement("h2", "", "No circles yet."),
          makeElement("p", "", "Create a circle to get started.")
        );
        list.append(empty);
        return;
      }

      circles.forEach((circle) => {
        const card = makeElement("article", "circle-card");
        const details = makeElement("div", "");
        details.append(
          makeElement("span", "circle-type", circle.purpose),
          makeElement("h2", "", circle.name),
          makeElement("p", "", circle.description),
          makeElement(
            "span",
            "circle-member-count",
            `${circle.memberCount} ${circle.memberCount === 1 ? "person" : "people"}`
          )
        );
        const actions = makeElement("div", "circle-card-actions");
        const detailsButton = makeElement("button", "button button-subtle", "View details");
        detailsButton.type = "button";
        detailsButton.addEventListener("click", async () => {
          detailsButton.disabled = true;
          try {
            showDetails(await window.SyndeoAPI.getCircle(circle.id));
          } catch (error) {
            handleFeatureError(error, list);
          } finally {
            detailsButton.disabled = false;
          }
        });

        const joined = circle.membershipStatus === "ACTIVE";
        const membershipButton = makeElement(
          "button",
          joined ? "button button-outline" : "button",
          joined ? "Leave circle" : "Join circle"
        );
        membershipButton.type = "button";
        membershipButton.setAttribute("aria-pressed", String(joined));
        membershipButton.disabled = busy;
        membershipButton.addEventListener("click", async () => {
          if (busy) return;
          busy = true;
          membershipButton.disabled = true;
          try {
            if (joined) {
              await window.SyndeoAPI.leaveCircle(circle.id);
            } else {
              await window.SyndeoAPI.joinCircle(circle.id);
            }
            if (await loadCircles()) {
              announce(joined ? "You left the circle." : "You joined the circle.", "success");
            }
          } catch (error) {
            handleFeatureError(error, list);
          } finally {
            busy = false;
            renderCircles();
          }
        });
        actions.append(detailsButton, membershipButton);
        card.append(details, actions);
        list.append(card);
      });
    }

    async function loadCircles() {
      if (!window.SyndeoAPI?.getToken()) {
        showLoginPrompt(list);
        return false;
      }
      list.replaceChildren(makeElement("p", "app-help", "Loading circles…"));
      try {
        const result = await window.SyndeoAPI.getCircles();
        circles = Array.isArray(result) ? result : [];
        list.querySelector(".feature-error")?.remove();
        renderCircles();
        return true;
      } catch (error) {
        list.replaceChildren();
        handleFeatureError(error, list);
        return false;
      }
    }

    toggleButton.addEventListener("click", () => {
      const opening = createPanel.hidden;
      createPanel.hidden = !opening;
      toggleButton.setAttribute("aria-expanded", String(opening));
      if (opening) document.querySelector("#circle-name").focus();
    });

    document.querySelector("#cancel-create-circle").addEventListener("click", () => {
      createPanel.hidden = true;
      toggleButton.setAttribute("aria-expanded", "false");
      circleForm.reset();
    });

    circleForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      if (busy || !circleForm.reportValidity()) return;
      const data = new FormData(circleForm);
      const submitButton = circleForm.querySelector('[type="submit"]');
      busy = true;
      submitButton.disabled = true;
      try {
        await window.SyndeoAPI.createCircle({
          name: String(data.get("name")).trim(),
          purpose: String(data.get("type")).trim(),
          description: String(data.get("description")).trim()
        });
        const refreshed = await loadCircles();
        circleForm.reset();
        createPanel.hidden = true;
        toggleButton.setAttribute("aria-expanded", "false");
        if (refreshed) announce("Circle created.", "success");
      } catch (error) {
        handleFeatureError(error, list);
      } finally {
        busy = false;
        submitButton.disabled = false;
        renderCircles();
      }
    });

    document.querySelectorAll("[data-close-dialog]").forEach((button) => {
      button.addEventListener("click", () => dialog.close());
    });
    dialog.addEventListener("click", (event) => {
      if (event.target === dialog) dialog.close();
    });
    loadCircles();
  }

  // ============================================================
  // DASHBOARD
  // ============================================================

  async function setupDashboard() {
    const heading = document.querySelector("#dashboard-greeting");
    if (!heading) return;

    const grid = document.querySelector(".dashboard-grid");
    if (!window.SyndeoAPI?.getToken()) {
      showLoginPrompt(grid);
      return;
    }

    try {
      const account = await window.SyndeoAPI.getMyProfile();
      const displayName =
        typeof account?.displayName === "string"
          ? account.displayName.trim()
          : "";
      heading.textContent = displayName
        ? `Welcome, ${displayName}.`
        : "Welcome to SYNDEO.";

      const completeness = [
        Boolean(displayName),
        Boolean(account.bio),
        Array.isArray(account.skills) && account.skills.length > 0,
        Array.isArray(account.interests) && account.interests.length > 0,
        Boolean(account.experienceLevel),
        Boolean(account.goals),
        Array.isArray(account.availability) && account.availability.length > 0
      ];
      const percent = Math.round(
        (completeness.filter(Boolean).length / completeness.length) * 100
      );

      const progress = document.querySelector("#profile-progress");
      const progressTrack = document.querySelector(".progress-track");
      const status = document.querySelector("#dashboard-profile-status");
      const copy = document.querySelector("#dashboard-profile-copy");

      progress.style.width = `${percent}%`;
      progressTrack.setAttribute("aria-valuenow", String(percent));
      status.textContent = `${percent}% of your profile filled in.`;
      copy.textContent = percent === 100
        ? "Your profile is ready to introduce what you bring. You can update it any time."
        : "Add details about your skills, interests, goals, and availability to build your profile.";
    } catch (error) {
      handleFeatureError(error, grid);
    }

  }

  // ============================================================
  // PROJECTS
  // ============================================================

  async function setupProjects() {
    const list = document.querySelector("#project-list");
    if (!list) return;

    const detail = document.querySelector("#project-detail");
    const createPanel = document.querySelector("#create-project-panel");
    const projectForm = document.querySelector("#project-form");
    const taskForm = document.querySelector("#task-form");
    const taskList = document.querySelector("#task-list");
    const circleSelect = document.querySelector("#project-circle");
    const taskAssignee = document.querySelector("#task-assignee");
    const projectCount = document.querySelector("#project-count");
    let projects = [];
    let circles = [];
    let selectedProject = null;
    let tasks = [];
    let busy = false;

    function circleName(circleId) {
      return circles.find((circle) => circle.id === circleId)?.name;
    }

    function getProgress(projectTasks) {
      const completed = projectTasks.filter((task) => task.status === "done").length;
      return {
        total: projectTasks.length,
        completed,
        percent: projectTasks.length
          ? Math.round((completed / projectTasks.length) * 100)
          : 0
      };
    }

    function renderProjects() {
      if (!window.SyndeoAPI?.getToken()) {
        detail.hidden = true;
        showLoginPrompt(list);
        return;
      }
      list.replaceChildren();
      projectCount.textContent =
        `${projects.length} ${projects.length === 1 ? "project" : "projects"}`;
      if (!projects.length) {
        const empty = makeElement("div", "empty-state");
        empty.append(
          makeElement("h2", "", "No projects yet."),
          makeElement("p", "", "Create a project to give a shared idea a place to begin.")
        );
        list.append(empty);
        detail.hidden = true;
        return;
      }

      projects.forEach((project) => {
        const card = makeElement("article", "project-card");
        const text = makeElement("div", "project-card-copy");
        text.append(
          makeElement("h2", "", project.name),
          makeElement("p", "", project.description)
        );
        const relatedCircle = circleName(project.circleId);
        if (relatedCircle) {
          text.append(makeElement("span", "project-related-circle", `Circle · ${relatedCircle}`));
        }

        const meta = makeElement("div", "project-card-progress");
        meta.append(makeElement(
          "span",
          "",
          `${project.memberCount} ${project.memberCount === 1 ? "member" : "members"} · ${project.status}`
        ));

        const openButton = makeElement("button", "button button-subtle", "Open project");
        openButton.type = "button";
        openButton.addEventListener("click", () => openProject(project.id));
        meta.append(openButton);

        const membershipButton = makeElement(
          "button",
          project.member ? "button button-outline" : "button",
          project.member ? "Leave project" : "Join project"
        );
        membershipButton.type = "button";
        membershipButton.disabled = busy;
        membershipButton.addEventListener("click", async () => {
          if (busy) return;
          busy = true;
          membershipButton.disabled = true;
          try {
            if (project.member) {
              await window.SyndeoAPI.leaveProject(project.id);
            } else {
              await window.SyndeoAPI.joinProject(project.id);
            }
            if (await loadProjects()) {
              announce(project.member ? "You left the project." : "You joined the project.", "success");
            }
          } catch (error) {
            handleFeatureError(error, list);
          } finally {
            busy = false;
            renderProjects();
          }
        });
        meta.append(membershipButton);
        card.append(text, meta);
        list.append(card);
      });

      if (selectedProject) {
        const refreshed = projects.find((project) => project.id === selectedProject.id);
        if (refreshed) {
          selectedProject = {
            ...selectedProject,
            ...refreshed,
            members: selectedProject.members || []
          };
        }
        if (!selectedProject.member) tasks = [];
        renderProjectDetail();
      }
    }

    function renderProjectDetail(taskError = "") {
      if (!selectedProject) {
        detail.hidden = true;
        return;
      }
      detail.hidden = false;
      document.querySelector("#project-detail-title").textContent = selectedProject.name;
      document.querySelector("#project-detail-description").textContent =
        selectedProject.description;
      document.querySelector("#project-circle-label").textContent =
        circleName(selectedProject.circleId) || "No circle associated with this project.";

      const progress = getProgress(tasks);
      document.querySelector("#project-progress-summary").textContent =
        progress.total
          ? `${progress.completed} of ${progress.total} tasks complete · ${progress.percent}%`
          : "No tasks yet";
      document.querySelector("#project-progress").style.width = `${progress.percent}%`;
      document.querySelector("#project-progress-track")
        .setAttribute("aria-valuenow", String(progress.percent));

      taskList.replaceChildren();
      if (taskError) {
        const errorState = makeElement("div", "empty-state");
        errorState.append(
          makeElement("h3", "", "Project tasks are unavailable."),
          makeElement("p", "", taskError)
        );
        taskList.append(errorState);
      } else if (!tasks.length) {
        const empty = makeElement("div", "empty-state");
        empty.append(
          makeElement("h3", "", "No tasks for this project yet."),
          makeElement("p", "", selectedProject.member
            ? "Add a task below to keep the next step visible."
            : "Join this project to view and create its tasks.")
        );
        taskList.append(empty);
      } else {
        tasks.forEach((task) => renderTask(task));
      }

      const canManage = Boolean(
        selectedProject.member && window.SyndeoAPI?.getToken()
      );
      const selectedAssignee = taskAssignee.value;
      taskAssignee.replaceChildren(new Option("Unassigned", ""));
      (selectedProject.members || []).forEach((member) => {
        taskAssignee.add(new Option(member.displayName, String(member.userId)));
      });
      taskAssignee.value = (selectedProject.members || [])
        .some((member) => String(member.userId) === selectedAssignee)
        ? selectedAssignee : "";
      taskForm.querySelectorAll("input, textarea, select, button").forEach((control) => {
        control.disabled = !canManage || Boolean(taskError) || busy;
      });
    }

    function renderTask(task) {
      const item = makeElement("article", "task-item");
      const copy = makeElement("div", "task-item-copy");
      copy.append(makeElement("h4", "", task.title));
      if (task.description) copy.append(makeElement("p", "", task.description));

      const assignee = selectedProject.members?.find(
        (member) => member.userId === task.assigneeUserId
      );
      copy.append(makeElement(
        "span",
        "task-assignee",
        assignee ? `Assigned to ${assignee.displayName}` :
          task.assigneeUserId ? `Assigned user #${task.assigneeUserId}` : "Unassigned"
      ));

      const controls = makeElement("div", "task-item-controls");
      const statusLabel = makeElement("label", "app-help", "Status");
      const statusSelect = document.createElement("select");
      statusSelect.className = `task-status task-status-${task.status}`;
      statusSelect.setAttribute("aria-label", `Status for ${task.title}`);
      taskStatuses.forEach((status) => statusSelect.add(new Option(status.label, status.value)));
      statusSelect.value = task.status;
      statusSelect.disabled = !selectedProject.member || busy;
      statusSelect.addEventListener("change", async () => {
        const previous = task.status;
        statusSelect.disabled = true;
        try {
          await window.SyndeoAPI.updateTask(
            selectedProject.id, task.id, { status: statusSelect.value });
          await refreshTasks();
          await loadProjects();
          announce("Task status updated.", "success");
        } catch (error) {
          statusSelect.value = previous;
          statusSelect.disabled = false;
          handleFeatureError(error, taskList);
        }
      });
      statusLabel.append(statusSelect);

      const assigneeLabel = makeElement("label", "app-help", "Assignee");
      const assigneeSelect = document.createElement("select");
      assigneeSelect.setAttribute("aria-label", `Assignee for ${task.title}`);
      assigneeSelect.add(new Option("Unassigned", ""));
      (selectedProject.members || []).forEach((member) => {
        assigneeSelect.add(new Option(member.displayName, String(member.userId)));
      });
      assigneeSelect.value = task.assigneeUserId == null
        ? "" : String(task.assigneeUserId);
      assigneeSelect.disabled = !selectedProject.member || busy;
      assigneeSelect.addEventListener("change", async () => {
        const previous = task.assigneeUserId == null ? "" : String(task.assigneeUserId);
        assigneeSelect.disabled = true;
        try {
          await window.SyndeoAPI.updateTask(selectedProject.id, task.id, {
            assigneeUserId: assigneeSelect.value ? Number(assigneeSelect.value) : null
          });
          await refreshTasks();
          await loadProjects();
          announce("Task assignment updated.", "success");
        } catch (error) {
          assigneeSelect.value = previous;
          assigneeSelect.disabled = false;
          handleFeatureError(error, taskList);
        }
      });
      assigneeLabel.append(assigneeSelect);
      controls.append(statusLabel, assigneeLabel);
      item.append(copy, controls);
      taskList.append(item);
    }

    async function refreshTasks() {
      if (!selectedProject) return;
      tasks = await window.SyndeoAPI.getTasks(selectedProject.id);
      renderProjectDetail();
    }

    async function openProject(projectId) {
      selectedProject = null;
      detail.hidden = false;
      document.querySelector("#project-detail-title").textContent = "Loading project…";
      taskList.replaceChildren(makeElement("p", "app-help", "Loading tasks…"));
      try {
        selectedProject = await window.SyndeoAPI.getProject(projectId);
        tasks = [];
        if (selectedProject.member) {
          tasks = await window.SyndeoAPI.getTasks(projectId);
          if (!Array.isArray(tasks)) tasks = [];
        }
        renderProjectDetail();
        detail.scrollIntoView({ behavior: "smooth", block: "start" });
      } catch (error) {
        if (selectedProject && error?.status === 403) {
          renderProjectDetail("Join this project to view its tasks.");
          detail.scrollIntoView({ behavior: "smooth", block: "start" });
          return;
        }
        detail.hidden = true;
        handleFeatureError(error, list);
      }
    }

    async function loadProjects() {
      if (!window.SyndeoAPI?.getToken()) {
        showLoginPrompt(list);
        return false;
      }
      list.replaceChildren(makeElement("p", "app-help", "Loading projects…"));
      try {
        const results = await Promise.all([
          window.SyndeoAPI.getProjects(),
          window.SyndeoAPI.getCircles()
        ]);
        projects = Array.isArray(results[0]) ? results[0] : [];
        circles = Array.isArray(results[1]) ? results[1] : [];
        list.querySelector(".feature-error")?.remove();
        const currentCircle = circleSelect.value;
        circleSelect.replaceChildren(new Option("No circle selected", ""));
        circles.filter((circle) => circle.membershipStatus === "ACTIVE").forEach((circle) => {
          circleSelect.add(new Option(circle.name, String(circle.id)));
        });
        if (circles.some((circle) =>
          circle.membershipStatus === "ACTIVE" &&
          String(circle.id) === currentCircle)) {
          circleSelect.value = currentCircle;
        }
        renderProjects();
        return true;
      } catch (error) {
        list.replaceChildren();
        handleFeatureError(error, list);
        return false;
      }
    }

    document.querySelector("#toggle-create-project").addEventListener("click", (event) => {
      const opening = createPanel.hidden;
      createPanel.hidden = !opening;
      event.currentTarget.setAttribute("aria-expanded", String(opening));
      if (opening) document.querySelector("#project-name").focus();
    });

    document.querySelector("#cancel-create-project").addEventListener("click", () => {
      createPanel.hidden = true;
      document.querySelector("#toggle-create-project")
        .setAttribute("aria-expanded", "false");
      projectForm.reset();
    });

    document.querySelector("#close-project").addEventListener("click", () => {
      selectedProject = null;
      detail.hidden = true;
    });

    projectForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      if (busy || !projectForm.reportValidity()) return;
      const data = new FormData(projectForm);
      const submitButton = projectForm.querySelector('[type="submit"]');
      busy = true;
      submitButton.disabled = true;
      try {
        const project = await window.SyndeoAPI.createProject({
          name: String(data.get("name")).trim(),
          description: String(data.get("description")).trim(),
          circleId: data.get("circleId") ? Number(data.get("circleId")) : null
        });
        selectedProject = project;
        await loadProjects();
        await openProject(project.id);
        projectForm.reset();
        createPanel.hidden = true;
        document.querySelector("#toggle-create-project")
          .setAttribute("aria-expanded", "false");
        announce("Project created.", "success");
      } catch (error) {
        handleFeatureError(error, list);
      } finally {
        busy = false;
        renderProjects();
        submitButton.disabled = false;
      }
    });

    taskForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      if (busy || !selectedProject || !taskForm.reportValidity()) return;
      const data = new FormData(taskForm);
      const submitButton = taskForm.querySelector('[type="submit"]');
      busy = true;
      submitButton.disabled = true;
      try {
        await window.SyndeoAPI.createTask(selectedProject.id, {
          title: String(data.get("title")).trim(),
          description: String(data.get("description")).trim() || null,
          assigneeUserId: data.get("assigneeId")
            ? Number(data.get("assigneeId")) : null
        });
        taskForm.reset();
        await refreshTasks();
        await loadProjects();
        announce("Task created.", "success");
      } catch (error) {
        handleFeatureError(error, taskList);
      } finally {
        busy = false;
        submitButton.disabled = false;
        renderProjects();
      }
    });

    taskAssignee.replaceChildren(new Option("Unassigned", ""));
    await loadProjects();
  }

  // ============================================================
  // INITIALIZE THE PAGE FEATURES
  // ============================================================

  setupProfile();
  setupAccountIdentity();
  setupDiscovery();
  setupCircles();
  setupProjects();
  setupDashboard();
})();
