(() => {
  const STORAGE_KEYS = {
    profile: "syndeo.demo.profile.v1",
    circles: "syndeo.demo.circles.v1",
    memberships: "syndeo.demo.circle-memberships.v1",
    projects: "syndeo.demo.projects.v1"
  };
  const MAX_PHOTO_BYTES = 1024 * 1024;
  const message = document.querySelector("#app-message");

  const collaborators = [
    {
      id: "maya-chen",
      name: "Maya Chen",
      initials: "MC",
      bio: "Curious about making everyday technology more useful and welcoming.",
      skills: ["UX research", "Prototyping", "Figma"],
      interests: ["Accessible design", "Education"],
      experience: "Some experience",
      goal: "Explore a small, human-centered learning tool.",
      availability: ["Weekends"],
      fit: "A research-minded teammate could pair well with Maya's prototyping practice to explore an idea from both the people and product sides."
    },
    {
      id: "elias-roberts",
      name: "Elias Roberts",
      initials: "ER",
      bio: "Likes turning environmental questions into small experiments and clear stories.",
      skills: ["Data visualization", "Python", "Research"],
      interests: ["Climate", "Open data"],
      experience: "Experienced",
      goal: "Make local environmental information easier to understand.",
      availability: ["Evenings", "Weekends"],
      fit: "Someone interested in communication or interface design could complement Elias's data work and help make an experiment easier to explore."
    },
    {
      id: "nina-patel",
      name: "Nina Patel",
      initials: "NP",
      bio: "Enjoys explaining tricky ideas and finding the little steps that help people learn.",
      skills: ["Peer tutoring", "Writing", "Workshop planning"],
      interests: ["Learning science", "Study groups"],
      experience: "Can mentor others",
      goal: "Try a friendly peer-learning format with other students.",
      availability: ["Weekdays", "Evenings"],
      fit: "A collaborator who enjoys building or organizing could help Nina turn a learning idea into a practical group activity."
    },
    {
      id: "sam-okafor",
      name: "Sam Okafor",
      initials: "SO",
      bio: "A hands-on maker who enjoys the path from rough idea to working prototype.",
      skills: ["Electronics", "CAD", "Rapid prototyping"],
      interests: ["Assistive technology", "Making"],
      experience: "Some experience",
      goal: "Build and test a useful prototype with a mixed-discipline team.",
      availability: ["Flexible"],
      fit: "A partner with research or accessibility experience could help connect Sam's practical prototyping with a clear user need."
    }
  ];

  const starterCircles = [
    {
      id: "circle-study-sprint",
      name: "The Gentle Study Sprint",
      type: "Study group",
      description: "A low-pressure space to set a small weekly learning goal, share what is confusing, and explain ideas to one another.",
      tags: ["Peer learning", "Study habits"],
      members: 5,
      owner: "SYNDEO demo"
    },
    {
      id: "circle-climate-build",
      name: "Small Climate Ideas",
      type: "Project team",
      description: "Explore a practical local sustainability question and see what a small research or prototype project could look like.",
      tags: ["Climate", "Research", "Prototyping"],
      members: 4,
      owner: "SYNDEO demo"
    },
    {
      id: "circle-design-peer",
      name: "Designing for Everyone",
      type: "Mentorship group",
      description: "Trade feedback on accessible design decisions and learn from different ways of approaching the same interface.",
      tags: ["Accessible design", "UX", "Feedback"],
      members: 6,
      owner: "SYNDEO demo"
    },
    {
      id: "circle-open-collab",
      name: "Open Collaboration Lab",
      type: "Professional collaboration",
      description: "Bring a skill you want to practice and find a small, clearly scoped idea to explore together.",
      tags: ["Making", "Teamwork", "Ideas"],
      members: 3,
      owner: "SYNDEO demo"
    }
  ];

  const starterProjects = [
    {
      id: "sample-campus-garden-guide",
      name: "Campus Garden Guide",
      description: "Collect practical ideas to help students find and care for small shared growing spaces.",
      circleId: "circle-climate-build",
      tasks: [
        { id: "garden-outline", title: "Outline the guide sections", description: "Agree on the useful basics a new volunteer needs.", status: "done", assigneeId: "nina-patel" },
        { id: "garden-interviews", title: "Gather student perspectives", description: "Prepare a few questions about joining a campus garden.", status: "in-progress", assigneeId: "maya-chen" },
        { id: "garden-resource-list", title: "List starter resources", description: "Find tools and local growing references to include.", status: "to-do", assigneeId: "" }
      ]
    },
    {
      id: "sample-study-notes",
      name: "Shared Study Notes",
      description: "Try a clear, welcoming format for turning course questions into notes students can learn from together.",
      circleId: "circle-study-sprint",
      tasks: [
        { id: "notes-format", title: "Sketch a note format", description: "Keep the structure easy to scan and contribute to.", status: "in-progress", assigneeId: "elias-roberts" },
        { id: "notes-topic", title: "Choose a first topic", description: "", status: "to-do", assigneeId: "" }
      ]
    }
  ];

  const sampleAssignees = [
    { id: "maya-chen", name: "Maya Chen" },
    { id: "elias-roberts", name: "Elias Roberts" },
    { id: "nina-patel", name: "Nina Patel" },
    { id: "sam-okafor", name: "Sam Okafor" }
  ];
  const taskStatuses = [
    { value: "to-do", label: "To do" },
    { value: "in-progress", label: "In progress" },
    { value: "done", label: "Done" }
  ];

  function announce(text, state) {
    if (!message) return;
    message.textContent = text;
    if (state) {
      message.dataset.state = state;
    } else {
      delete message.dataset.state;
    }
  }

  function readStored(key, fallback) {
    try {
      const saved = window.localStorage.getItem(key);
      return saved === null ? fallback : JSON.parse(saved);
    } catch (error) {
      console.error(`Could not read local demo data for ${key}.`, error);
      announce("Browser storage could not be read. This demo will continue without loading saved data.", "error");
      return fallback;
    }
  }

  function writeStored(key, value) {
    try {
      window.localStorage.setItem(key, JSON.stringify(value));
      return true;
    } catch (error) {
      console.error(`Could not save local demo data for ${key}.`, error);
      announce("This browser could not save the demo data. Your current page still shows it, but it may not be available after leaving.", "error");
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
    values.forEach((value) => container.append(makeElement("span", "tag-chip", value)));
  }

  function addChip(list, value) {
    const cleanValue = value.trim().replace(/\s+/g, " ");
    if (!cleanValue) return false;
    if (list.some((entry) => entry.toLocaleLowerCase() === cleanValue.toLocaleLowerCase())) {
      announce(`“${cleanValue}” is already in this list.`);
      return false;
    }
    list.push(cleanValue);
    return true;
  }

  function renderEditableChips(values, container, label, onRemove) {
    container.replaceChildren();
    if (!values.length) {
      container.append(makeElement("span", "app-help", `No ${label.toLowerCase()} added yet.`));
      return;
    }
    values.forEach((value, index) => {
      const chip = makeElement("span", "tag-chip");
      chip.append(document.createTextNode(value));
      const remove = makeElement("button", "tag-chip-remove", "×");
      remove.type = "button";
      remove.setAttribute("aria-label", `Remove ${value} ${label.toLowerCase()}`);
      remove.addEventListener("click", () => onRemove(index));
      chip.append(remove);
      container.append(chip);
    });
  }

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

    const saved = readStored(STORAGE_KEYS.profile, null);
    const state = {
      skills: Array.isArray(saved && saved.skills) ? saved.skills.filter((item) => typeof item === "string") : [],
      interests: Array.isArray(saved && saved.interests) ? saved.interests.filter((item) => typeof item === "string") : [],
      availability: Array.isArray(saved && saved.availability) ? saved.availability.filter((item) => typeof item === "string") : [],
      photo: saved && typeof saved.photo === "string" ? saved.photo : ""
    };

    fields.name.value = saved && typeof saved.name === "string" ? saved.name : "";
    fields.bio.value = saved && typeof saved.bio === "string" ? saved.bio : "";
    fields.experience.value = saved && typeof saved.experience === "string" ? saved.experience : "";
    fields.goal.value = saved && typeof saved.goal === "string" ? saved.goal : "";
    form.querySelectorAll('input[name="availability"]').forEach((input) => {
      input.checked = state.availability.includes(input.value);
    });

    function renderPreview() {
      const name = fields.name.value.trim();
      document.querySelector("#profile-preview-title").textContent = name || "Your profile";
      document.querySelector("#preview-experience").textContent = fields.experience.value || "Add details to introduce yourself.";
      document.querySelector("#preview-bio").textContent = fields.bio.value.trim() || "Your introduction will appear here.";
      document.querySelector("#preview-goal").textContent = fields.goal.value.trim() || "Add a goal when you are ready.";
      renderTags(document.querySelector("#preview-skills"), state.skills, "Add a skill to introduce what you bring.");
      renderTags(document.querySelector("#preview-interests"), state.interests, "Add interests to show what you care about.");
      renderTags(
        document.querySelector("#preview-availability"),
        [...form.querySelectorAll('input[name="availability"]:checked')].map((input) => input.value),
        "Choose when you might collaborate."
      );
      renderAvatar(fields.avatar, name, state.photo, false);
      fields.removePhoto.hidden = !state.photo;
      renderEditableChips(state.skills, fields.skills, "Skills", (index) => {
        state.skills.splice(index, 1);
        renderPreview();
      });
      renderEditableChips(state.interests, fields.interests, "Interests", (index) => {
        state.interests.splice(index, 1);
        renderPreview();
      });
    }

    function addEntry(input, values, list, label) {
      if (!addChip(values, input.value)) return;
      input.value = "";
      renderPreview();
      announce(`${label} added.`);
    }

    document.querySelector("#add-skill").addEventListener("click", () => {
      addEntry(fields.skillEntry, state.skills, fields.skills, "Skill");
    });
    document.querySelector("#add-interest").addEventListener("click", () => {
      addEntry(fields.interestEntry, state.interests, fields.interests, "Interest");
    });

    [[fields.skillEntry, "#add-skill"], [fields.interestEntry, "#add-interest"]].forEach(([input, buttonSelector]) => {
      input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === ",") {
          event.preventDefault();
          document.querySelector(buttonSelector).click();
        }
      });
    });

    [fields.name, fields.bio, fields.experience, fields.goal].forEach((input) => {
      input.addEventListener("input", renderPreview);
      input.addEventListener("change", renderPreview);
    });
    form.querySelectorAll('input[name="availability"]').forEach((input) => input.addEventListener("change", renderPreview));

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
        announce("Picture preview updated. Save your profile to keep it in this browser.", "success");
      });
      reader.addEventListener("error", () => {
        console.error("Could not read the selected profile picture.", reader.error);
        announce("The selected picture could not be read. Please try another image.", "error");
      });
      reader.readAsDataURL(file);
    });

    fields.removePhoto.addEventListener("click", () => {
      state.photo = "";
      fields.photo.value = "";
      renderPreview();
      announce("Picture removed from the preview. Save your profile to keep this change.", "success");
    });

    form.addEventListener("submit", (event) => {
      event.preventDefault();
      fields.skillEntry.setCustomValidity(state.skills.length ? "" : "Add at least one skill to your profile.");
      if (!form.reportValidity()) return;

      const profile = {
        name: fields.name.value.trim(),
        bio: fields.bio.value.trim(),
        skills: [...state.skills],
        interests: [...state.interests],
        experience: fields.experience.value,
        goal: fields.goal.value.trim(),
        availability: [...form.querySelectorAll('input[name="availability"]:checked')].map((input) => input.value),
        photo: state.photo
      };
      if (writeStored(STORAGE_KEYS.profile, profile)) {
        announce("Profile saved in this browser only. It has not been synced to SYNDEO.", "success");
      }
    });

    fields.skillEntry.addEventListener("input", () => fields.skillEntry.setCustomValidity(""));
    renderPreview();
  }

  function appendProfileTags(parent, heading, values) {
    parent.append(makeElement("h3", "", heading));
    const tags = makeElement("div", "tag-list");
    renderTags(tags, values, "Not listed");
    parent.append(tags);
  }

  function setupDiscovery() {
    const list = document.querySelector("#collaborator-list");
    if (!list) return;

    const skillFilter = document.querySelector("#skill-filter");
    const interestFilter = document.querySelector("#interest-filter");
    const search = document.querySelector("#collaborator-search");
    const dialog = document.querySelector("#detail-dialog");
    const detailBody = document.querySelector("#detail-body");
    const profile = readStored(STORAGE_KEYS.profile, null);

    [...new Set(collaborators.flatMap((person) => person.skills))].sort().forEach((skill) => {
      skillFilter.append(new Option(skill, skill));
    });
    [...new Set(collaborators.flatMap((person) => person.interests))].sort().forEach((interest) => {
      interestFilter.append(new Option(interest, interest));
    });

    function openPerson(person) {
      detailBody.replaceChildren();
      const heading = makeElement("h2", "", person.name);
      heading.id = "detail-title";
      const bio = makeElement("p", "", person.bio);
      const goal = makeElement("p", "", `Hoping to: ${person.goal}`);
      const availability = makeElement("p", "", `Availability: ${person.availability.join(", ")}`);
      detailBody.append(heading, bio);
      appendProfileTags(detailBody, "Skills they bring", person.skills);
      appendProfileTags(detailBody, "Interests", person.interests);
      detailBody.append(makeElement("h3", "", "Experience"), makeElement("p", "", person.experience));
      detailBody.append(makeElement("h3", "", "Looking to"), goal);
      detailBody.append(makeElement("h3", "", "Availability"), availability);
      detailBody.append(makeElement("p", "fit-note", person.fit));
      const circleLink = makeElement("a", "button", "Explore circles");
      circleLink.href = "circles.html";
      circleLink.dataset.pageTransition = "";
      detailBody.append(circleLink);
      if (typeof dialog.showModal === "function") dialog.showModal();
    }

    function renderPeople() {
      const query = search.value.trim().toLocaleLowerCase();
      const skill = skillFilter.value;
      const interest = interestFilter.value;
      const filtered = collaborators.filter((person) => {
        const searchable = [person.name, person.bio, person.goal, ...person.skills, ...person.interests].join(" ").toLocaleLowerCase();
        return (!query || searchable.includes(query)) &&
          (!skill || person.skills.includes(skill)) &&
          (!interest || person.interests.includes(interest));
      });

      list.replaceChildren();
      document.querySelector("#result-count").textContent = `${filtered.length} fictional profile${filtered.length === 1 ? "" : "s"}`;

      if (!filtered.length) {
        const empty = makeElement("div", "empty-state");
        empty.append(makeElement("h2", "", "No examples match those filters."), makeElement("p", "", "Try a different search or clear one of the filters."));
        list.append(empty);
        return;
      }

      filtered.forEach((person) => {
        const card = makeElement("article", "collaborator-card");
        const avatar = makeElement("div", "person-avatar", person.initials);
        avatar.setAttribute("aria-hidden", "true");
        const main = makeElement("div", "collaborator-main");
        main.append(
          makeElement("h2", "", person.name),
          makeElement("p", "collaborator-meta", `${person.experience} · ${person.availability.join(", ")}`),
          makeElement("p", "", person.bio)
        );
        main.append(makeElement("h3", "card-section-label", "Skills they bring"));
        const skillTags = makeElement("div", "card-tags");
        person.skills.forEach((item) => skillTags.append(makeElement("span", "tag-chip", item)));
        const interests = makeElement("p", "app-help", `Interested in ${person.interests.join(" · ")}`);
        main.append(skillTags, interests);

        const actions = makeElement("div", "collaborator-actions");
        const fit = makeElement("p", "fit-note", person.fit);
        if (profile && Array.isArray(profile.skills) && profile.skills.length) {
          const shared = profile.skills.filter((item) => person.skills.some((theirSkill) => theirSkill.toLocaleLowerCase() === item.toLocaleLowerCase()));
          const note = shared.length
            ? `Your profile lists ${shared.join(", ")} too; this example is still illustrative, not an engine result.`
            : "Compare strengths and interests; this example is not an engine result.";
          fit.textContent = `${note} ${person.fit}`;
        }
        actions.append(makeElement("h3", "card-section-label", "Possible complementary contribution"));
        const detailButton = makeElement("button", "button button-subtle", "View profile");
        detailButton.type = "button";
        detailButton.addEventListener("click", () => openPerson(person));
        const circleLink = makeElement("a", "button", "Explore circles");
        circleLink.href = "circles.html";
        circleLink.dataset.pageTransition = "";
        actions.append(makeElement("span", "demo-label", "Illustrative collaboration fit"), fit, detailButton, circleLink);
        card.append(avatar, main, actions);
        list.append(card);
      });
    }

    [search, skillFilter, interestFilter].forEach((control) => control.addEventListener("input", renderPeople));
    [skillFilter, interestFilter].forEach((control) => control.addEventListener("change", renderPeople));
    document.querySelectorAll("[data-close-dialog]").forEach((button) => button.addEventListener("click", () => dialog.close()));
    dialog.addEventListener("click", (event) => {
      if (event.target === dialog) dialog.close();
    });
    renderPeople();
  }

  function setupCircles() {
    const list = document.querySelector("#circle-list");
    if (!list) return;

    const dialog = document.querySelector("#detail-dialog");
    const detailBody = document.querySelector("#detail-body");
    const savedCircles = readStored(STORAGE_KEYS.circles, []);
    const savedMemberships = readStored(STORAGE_KEYS.memberships, []);
    const created = Array.isArray(savedCircles) ? savedCircles.filter((circle) => circle && typeof circle.id === "string") : [];
    const memberships = new Set(Array.isArray(savedMemberships) ? savedMemberships.filter((id) => typeof id === "string") : []);
    let circles = [...starterCircles, ...created];

    function persistMemberships() {
      writeStored(STORAGE_KEYS.memberships, [...memberships]);
    }

    function showDetails(circle) {
      detailBody.replaceChildren();
      const heading = makeElement("h2", "", circle.name);
      heading.id = "detail-title";
      detailBody.append(
        makeElement("span", "circle-type", circle.type),
        heading,
        makeElement("p", "", circle.description)
      );
      appendProfileTags(detailBody, "Skills and interests", circle.tags || []);
      const memberCount = circle.members + (circle.localOwner || !memberships.has(circle.id) ? 0 : 1);
      detailBody.append(
        makeElement("h3", "", "Group size"),
        makeElement("p", "", `${memberCount} ${memberCount === 1 ? "person" : "people"} in this local demo · ${circle.owner || "Fictional sample group"}`),
        makeElement("p", "demo-label", "This is a fictional/local demo group. No membership is sent to a server.")
      );
      if (typeof dialog.showModal === "function") dialog.showModal();
    }

    function renderCircles() {
      list.replaceChildren();
      document.querySelector("#circle-count").textContent = `${circles.length} circles in this local demo`;
      if (!circles.length) {
        const empty = makeElement("div", "empty-state");
        empty.append(makeElement("h2", "", "No circles yet."), makeElement("p", "", "Create a local demo circle to start sketching a shared purpose."));
        list.append(empty);
        return;
      }

      circles.forEach((circle) => {
        const card = makeElement("article", "circle-card");
        const details = makeElement("div", "");
        details.append(
          makeElement("span", "circle-type", circle.type),
          makeElement("h2", "", circle.name),
          makeElement("p", "", circle.description)
        );
        const tags = makeElement("div", "card-tags");
        (circle.tags || []).forEach((tag) => tags.append(makeElement("span", "tag-chip", tag)));
        const memberCount = circle.members + (circle.localOwner || !memberships.has(circle.id) ? 0 : 1);
        details.append(tags, makeElement("span", "demo-label", `${memberCount} ${memberCount === 1 ? "person" : "people"} in this local demo`));

        const actions = makeElement("div", "circle-card-actions");
        const detailsButton = makeElement("button", "button button-subtle", "View details");
        detailsButton.type = "button";
        detailsButton.addEventListener("click", () => showDetails(circle));

        const joined = memberships.has(circle.id);
        const membershipButton = makeElement("button", joined ? "button button-outline" : "button", joined ? "Leave demo circle" : "Join demo circle");
        membershipButton.type = "button";
        membershipButton.setAttribute("aria-pressed", String(joined));
        membershipButton.addEventListener("click", () => {
          if (memberships.has(circle.id)) {
            memberships.delete(circle.id);
            announce(`Left “${circle.name}” in this local demo.`, "success");
          } else {
            memberships.add(circle.id);
            announce(`Joined “${circle.name}” in this local demo only. No server action occurred.`, "success");
          }
          persistMemberships();
          renderCircles();
        });
        actions.append(detailsButton, membershipButton);
        card.append(details, actions);
        list.append(card);
      });
    }

    const createPanel = document.querySelector("#create-circle-panel");
    const toggleButton = document.querySelector("#toggle-create-circle");
    const circleForm = document.querySelector("#circle-form");
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

    circleForm.addEventListener("submit", (event) => {
      event.preventDefault();
      if (!circleForm.reportValidity()) return;
      const data = new FormData(circleForm);
      const circle = {
        id: `local-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        name: String(data.get("name")).trim(),
        type: String(data.get("type")),
        description: String(data.get("description")).trim(),
        tags: String(data.get("tags")).split(",").map((tag) => tag.trim()).filter(Boolean).slice(0, 8),
        members: 1,
        localOwner: true,
        owner: "Your local demo"
      };
      if (circles.some((item) => item.name.toLocaleLowerCase() === circle.name.toLocaleLowerCase())) {
        announce("A circle with that name is already listed. Choose a different name.", "error");
        document.querySelector("#circle-name").focus();
        return;
      }
      created.push(circle);
      memberships.add(circle.id);
      circles = [...starterCircles, ...created];
      const saved = writeStored(STORAGE_KEYS.circles, created);
      persistMemberships();
      circleForm.reset();
      createPanel.hidden = true;
      toggleButton.setAttribute("aria-expanded", "false");
      renderCircles();
      if (saved) announce(`“${circle.name}” was added to this browser's local demo only. It was not created on a server.`, "success");
    });

    document.querySelectorAll("[data-close-dialog]").forEach((button) => button.addEventListener("click", () => dialog.close()));
    dialog.addEventListener("click", (event) => {
      if (event.target === dialog) dialog.close();
    });
    renderCircles();
  }

  function setupDashboard() {
    const heading = document.querySelector("#dashboard-greeting");
    if (!heading) return;
    const profile = readStored(STORAGE_KEYS.profile, null);
    if (!profile) return;

    const name = typeof profile.name === "string" ? profile.name.trim() : "";
    if (name) heading.textContent = `Welcome, ${name}.`;
    const completeness = [
      Boolean(name),
      Boolean(profile.bio),
      Array.isArray(profile.skills) && profile.skills.length > 0,
      Array.isArray(profile.interests) && profile.interests.length > 0,
      Boolean(profile.experience),
      Boolean(profile.goal),
      Array.isArray(profile.availability) && profile.availability.length > 0
    ].filter(Boolean).length;
    const percent = Math.round((completeness / 6) * 100);
    document.querySelector("#profile-progress").style.width = `${percent}%`;
    document.querySelector(".progress-track").setAttribute("aria-valuenow", String(percent));
    document.querySelector("#dashboard-profile-status").textContent = `${percent}% of your profile filled in.`;
    document.querySelector("#dashboard-profile-copy").textContent = percent === 100
      ? "Your profile is ready to introduce what you bring. You can update it any time."
      : "Keep adding details about your skills and interests to make your introduction more useful.";
  }

  function setupProjects() {
    const list = document.querySelector("#project-list");
    if (!list) return;

    const detail = document.querySelector("#project-detail");
    const createPanel = document.querySelector("#create-project-panel");
    const projectForm = document.querySelector("#project-form");
    const taskForm = document.querySelector("#task-form");
    const taskList = document.querySelector("#task-list");
    const circleSelect = document.querySelector("#project-circle");
    const taskAssignee = document.querySelector("#task-assignee");
    let projects = readStored(STORAGE_KEYS.projects, null);

    function validProject(project) {
      return project &&
        typeof project.id === "string" &&
        typeof project.name === "string" &&
        typeof project.description === "string" &&
        (typeof project.circleId === "string" || project.circleId === "") &&
        Array.isArray(project.tasks) &&
        project.tasks.every((task) =>
          task &&
          typeof task.id === "string" &&
          typeof task.title === "string" &&
          typeof task.description === "string" &&
          taskStatuses.some((status) => status.value === task.status) &&
          (typeof task.assigneeId === "string" || task.assigneeId === "")
        );
    }

    if (!Array.isArray(projects) || !projects.every(validProject)) {
      if (projects !== null) {
        console.error("Stored project demo data has an invalid shape; using the fictional sample projects.");
        announce("Saved project demo data could not be used. The sample projects are shown instead.", "error");
      }
      projects = starterProjects.map((project) => ({
        ...project,
        tasks: project.tasks.map((task) => ({ ...task }))
      }));
    }

    const savedCircles = readStored(STORAGE_KEYS.circles, []);
    const availableCircles = [
      ...starterCircles,
      ...(Array.isArray(savedCircles) ? savedCircles.filter((circle) =>
        circle && typeof circle.id === "string" && typeof circle.name === "string"
      ) : [])
    ];
    const circleNames = new Map(availableCircles.map((circle) => [circle.id, circle.name]));
    availableCircles.forEach((circle) => circleSelect.add(new Option(circle.name, circle.id)));
    sampleAssignees.forEach((person) => {
      if (!taskAssignee.querySelector(`option[value="${person.id}"]`)) {
        taskAssignee.add(new Option(`${person.name} · fictional demo`, person.id));
      }
    });

    let selectedProjectId = projects[0] ? projects[0].id : "";

    function getProgress(project) {
      const total = project.tasks.length;
      const completed = project.tasks.filter((task) => task.status === "done").length;
      return {
        total,
        completed,
        percent: total ? Math.round((completed / total) * 100) : 0
      };
    }

    function persistProjects() {
      return writeStored(STORAGE_KEYS.projects, projects);
    }

    function openProject(projectId) {
      selectedProjectId = projectId;
      renderProjects();
      detail.scrollIntoView({ behavior: "smooth", block: "start" });
      document.querySelector("#task-title").focus({ preventScroll: true });
    }

    function renderProjects() {
      list.replaceChildren();
      document.querySelector("#project-count").textContent =
        `${projects.length} ${projects.length === 1 ? "project" : "projects"} in this local demo`;

      if (!projects.length) {
        const empty = makeElement("div", "empty-state");
        empty.append(
          makeElement("h2", "", "No projects yet."),
          makeElement("p", "", "Create a local demo project to give a shared idea a place to begin.")
        );
        list.append(empty);
      }

      projects.forEach((project) => {
        const progress = getProgress(project);
        const card = makeElement("article", "project-card");
        const text = makeElement("div", "project-card-copy");
        text.append(
          makeElement("h2", "", project.name),
          makeElement("p", "", project.description)
        );
        if (project.circleId && circleNames.has(project.circleId)) {
          text.append(makeElement("span", "project-related-circle", `Circle · ${circleNames.get(project.circleId)}`));
        }

        const meta = makeElement("div", "project-card-progress");
        const progressCopy = makeElement("span", "", `${progress.completed} of ${progress.total} tasks done`);
        const bar = makeElement("div", "progress-track");
        bar.setAttribute("role", "progressbar");
        bar.setAttribute("aria-label", `${project.name} task completion`);
        bar.setAttribute("aria-valuemin", "0");
        bar.setAttribute("aria-valuemax", "100");
        bar.setAttribute("aria-valuenow", String(progress.percent));
        const fill = makeElement("span");
        fill.style.width = `${progress.percent}%`;
        bar.append(fill);
        const button = makeElement("button", "button button-subtle", "Open project");
        button.type = "button";
        button.setAttribute("aria-pressed", String(project.id === selectedProjectId));
        button.addEventListener("click", () => openProject(project.id));
        meta.append(progressCopy, bar, button);
        card.append(text, meta);
        list.append(card);
      });

      const project = projects.find((entry) => entry.id === selectedProjectId);
      detail.hidden = !project;
      if (project) renderProjectDetail(project);
    }

    function renderProjectDetail(project) {
      const progress = getProgress(project);
      document.querySelector("#project-detail-title").textContent = project.name;
      document.querySelector("#project-detail-description").textContent = project.description;
      const circleLabel = document.querySelector("#project-circle-label");
      circleLabel.textContent = project.circleId && circleNames.has(project.circleId)
        ? `Related circle: ${circleNames.get(project.circleId)} (fictional demo)`
        : "No circle associated with this project.";
      document.querySelector("#project-progress-summary").textContent =
        progress.total
          ? `${progress.completed} of ${progress.total} tasks complete · ${progress.percent}%`
          : "No tasks yet";
      document.querySelector("#project-progress").style.width = `${progress.percent}%`;
      document.querySelector("#project-progress-track").setAttribute("aria-valuenow", String(progress.percent));
      taskList.replaceChildren();

      if (!project.tasks.length) {
        const empty = makeElement("div", "empty-state");
        empty.append(makeElement("h3", "", "No tasks for this project yet."), makeElement("p", "", "Add a task below to keep the next step visible."));
        taskList.append(empty);
        return;
      }

      project.tasks.forEach((task) => {
        const item = makeElement("article", "task-item");
        const copy = makeElement("div", "task-item-copy");
        copy.append(makeElement("h4", "", task.title));
        if (task.description) copy.append(makeElement("p", "", task.description));
        const assignee = sampleAssignees.find((person) => person.id === task.assigneeId);
        copy.append(makeElement(
          "span",
          "task-assignee",
          assignee ? `Sample assignee · ${assignee.name} (fictional)` : "Unassigned"
        ));

        const controls = makeElement("div", "task-item-controls");
        const statusLabel = makeElement("label", "app-help", "Status");
        const statusSelect = document.createElement("select");
        statusSelect.className = `task-status task-status-${task.status}`;
        statusSelect.setAttribute("aria-label", `Status for ${task.title}`);
        taskStatuses.forEach((status) => statusSelect.add(new Option(status.label, status.value)));
        statusSelect.value = task.status;
        statusSelect.addEventListener("change", () => {
          task.status = statusSelect.value;
          const statusSelectId = statusSelect.id;
          if (persistProjects()) {
            announce("Task status updated in this browser's local demo.", "success");
          }
          renderProjects();
          document.getElementById(statusSelectId)?.focus();
        });
        statusSelect.id = `task-status-${task.id}`;
        statusLabel.append(statusSelect);
        controls.append(statusLabel);
        item.append(copy, controls);
        taskList.append(item);
      });
    }

    document.querySelector("#toggle-create-project").addEventListener("click", (event) => {
      const opening = createPanel.hidden;
      createPanel.hidden = !opening;
      event.currentTarget.setAttribute("aria-expanded", String(opening));
      if (opening) document.querySelector("#project-name").focus();
    });
    document.querySelector("#cancel-create-project").addEventListener("click", () => {
      createPanel.hidden = true;
      document.querySelector("#toggle-create-project").setAttribute("aria-expanded", "false");
      projectForm.reset();
    });
    document.querySelector("#project-name").addEventListener("input", (event) => {
      event.currentTarget.setCustomValidity("");
    });
    document.querySelector("#project-description").addEventListener("input", (event) => {
      event.currentTarget.setCustomValidity("");
    });
    document.querySelector("#task-title").addEventListener("input", (event) => {
      event.currentTarget.setCustomValidity("");
    });
    document.querySelector("#close-project").addEventListener("click", () => {
      selectedProjectId = "";
      detail.hidden = true;
      list.querySelector("button[aria-pressed='true']")?.focus();
    });

    projectForm.addEventListener("submit", (event) => {
      event.preventDefault();
      const nameField = document.querySelector("#project-name");
      const descriptionField = document.querySelector("#project-description");
      nameField.setCustomValidity(nameField.value.trim() ? "" : "Enter a project name.");
      descriptionField.setCustomValidity(descriptionField.value.trim() ? "" : "Enter a short project description.");
      if (!projectForm.reportValidity()) return;
      const data = new FormData(projectForm);
      const name = String(data.get("name")).trim();
      if (projects.some((project) => project.name.toLocaleLowerCase() === name.toLocaleLowerCase())) {
        announce("A project with that name is already listed. Choose a different name.", "error");
        document.querySelector("#project-name").focus();
        return;
      }
      const project = {
        id: `local-project-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        name,
        description: String(data.get("description")).trim(),
        circleId: String(data.get("circleId") || ""),
        tasks: []
      };
      projects.push(project);
      const saved = persistProjects();
      projectForm.reset();
      createPanel.hidden = true;
      document.querySelector("#toggle-create-project").setAttribute("aria-expanded", "false");
      selectedProjectId = project.id;
      renderProjects();
      if (saved) announce(`“${project.name}” was added to this browser's demo only. It was not saved to a server.`, "success");
      detail.scrollIntoView({ behavior: "smooth", block: "start" });
    });

    taskForm.addEventListener("submit", (event) => {
      event.preventDefault();
      const titleField = document.querySelector("#task-title");
      titleField.setCustomValidity(titleField.value.trim() ? "" : "Enter a task title.");
      if (!taskForm.reportValidity()) return;
      const project = projects.find((entry) => entry.id === selectedProjectId);
      if (!project) {
        announce("Open a project before adding a task.", "error");
        return;
      }
      const data = new FormData(taskForm);
      const task = {
        id: `local-task-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        title: String(data.get("title")).trim(),
        description: String(data.get("description")).trim(),
        status: "to-do",
        assigneeId: String(data.get("assigneeId") || "")
      };
      project.tasks.push(task);
      const saved = persistProjects();
      taskForm.reset();
      renderProjects();
      if (saved) announce("Task added to this browser's local demo only. No teammate was notified.", "success");
      document.querySelector("#task-title").focus();
    });

    renderProjects();
  }

  setupProfile();
  setupDiscovery();
  setupCircles();
  setupProjects();
  setupDashboard();
})();