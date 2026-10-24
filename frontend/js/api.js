
(() => {
 const API_BASE_URL = "https://syndeo-jdhp.onrender.com/api";
  const TOKEN_KEY = "syndeo.accessToken.v1";

  async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    const token = sessionStorage.getItem(TOKEN_KEY);

    if (options.body !== undefined) {
      headers.set("Content-Type", "application/json");
    }

    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }

    let response;

    try {
      response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers
      });
    } catch {
      throw new Error(
        "Cannot reach SYNDEO's backend. Check that Spring Boot is running on port 8080."
      );
    }

    const contentType = response.headers.get("content-type") || "";
    const data = contentType.includes("application/json")
      ? await response.json()
      : null;

    if (!response.ok) {
      const error = new Error(
        data?.message ||
        `Request failed with HTTP ${response.status}.`
      );
      error.status = response.status;
      error.code = data?.code;
      throw error;
    }

    return data;
  }

  window.SyndeoAPI = Object.freeze({
    login(email, password) {
      return request("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password })
      });
    },

    register({ email, password, displayName, accountType }) {
      return request("/auth/register", {
        method: "POST",
        body: JSON.stringify({
          email,
          password,
          displayName,
          accountType
        })
      });
    },
    getMyProfile() {
    return request("/users/me");
    },
    updateMyProfile(profile) {
  return request("/users/me", {
    method: "PUT",
    body: JSON.stringify(profile)
  });
},
    getDiscoveryProfiles({ search, skill, interest } = {}) {
      const params = new URLSearchParams();
      if (search) params.set("search", search);
      if (skill) params.set("skill", skill);
      if (interest) params.set("interest", interest);
      const query = params.toString();
      return request(`/discovery${query ? `?${query}` : ""}`);
    },
    getCircles() {
      return request("/circles");
    },
    getCircle(circleId) {
      return request(`/circles/${encodeURIComponent(circleId)}`);
    },
    createCircle(circle) {
      return request("/circles", {
        method: "POST",
        body: JSON.stringify(circle)
      });
    },
    joinCircle(circleId) {
      return request(`/circles/${encodeURIComponent(circleId)}/join`, {
        method: "POST"
      });
    },
    leaveCircle(circleId) {
      return request(`/circles/${encodeURIComponent(circleId)}/leave`, {
        method: "POST"
      });
    },
    getProjects() {
      return request("/projects");
    },
    getProject(projectId) {
      return request(`/projects/${encodeURIComponent(projectId)}`);
    },
    createProject(project) {
      return request("/projects", {
        method: "POST",
        body: JSON.stringify(project)
      });
    },
    joinProject(projectId) {
      return request(`/projects/${encodeURIComponent(projectId)}/join`, {
        method: "POST"
      });
    },
    leaveProject(projectId) {
      return request(`/projects/${encodeURIComponent(projectId)}/membership`, {
        method: "DELETE"
      });
    },
    getTasks(projectId) {
      return request(`/projects/${encodeURIComponent(projectId)}/tasks`);
    },
    createTask(projectId, task) {
      return request(`/projects/${encodeURIComponent(projectId)}/tasks`, {
        method: "POST",
        body: JSON.stringify(task)
      });
    },
    updateTask(projectId, taskId, updates) {
      return request(
        `/projects/${encodeURIComponent(projectId)}/tasks/${encodeURIComponent(taskId)}`,
        {
          method: "PATCH",
          body: JSON.stringify(updates)
        }
      );
    },
    getToken() {
      return sessionStorage.getItem(TOKEN_KEY);
    },

    saveToken(token) {
      if (!token) throw new Error("The server did not return an access token.");
      sessionStorage.setItem(TOKEN_KEY, token);
    },

    clearToken() {
      sessionStorage.removeItem(TOKEN_KEY);
    }
  });
})();
