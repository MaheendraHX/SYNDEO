
const loginForm = document.querySelector("#login-form");
const loginMessage = document.querySelector("#login-message");

const registerForm = document.querySelector("#register-form");
const registerMessage = document.querySelector("#register-message");

function showMessage(element, message, isError = false) {
  if (!element) return;

  element.textContent = message;
  element.dataset.state = isError ? "error" : "success";
}

function setSubmitting(form, submitting) {
  const button = form.querySelector('button[type="submit"]');
  if (button) button.disabled = submitting;
}

// Login
if (loginForm && loginMessage) {
  loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    if (!loginForm.reportValidity()) return;

    const submitButton = loginForm.querySelector('button[type="submit"]');
    const originalText = submitButton?.textContent;

    setSubmitting(loginForm, true);
    showMessage(loginMessage, "Signing you in…");

    try {
      const email = loginForm.elements.email.value.trim();
      const password = loginForm.elements.password.value;

      const result = await window.SyndeoAPI.login(email, password);

      if (!result.accessToken) {
        throw new Error("The server did not return an access token.");
      }

      window.SyndeoAPI.saveToken(result.accessToken);
      showMessage(loginMessage, "Login successful. Opening SYNDEO…");

      window.location.href = "dashboard.html";
    } catch (error) {
      window.SyndeoAPI.clearToken();
      showMessage(
        loginMessage,
        error.message || "Login failed. Please try again.",
        true
      );
    } finally {
      setSubmitting(loginForm, false);
      if (submitButton && originalText) {
        submitButton.textContent = originalText;
      }
    }
  });
}

// Registration
if (registerForm && registerMessage) {
  const passwordInput = registerForm.elements.password;
  const confirmPasswordInput = registerForm.elements.confirmPassword;

  function validatePasswords() {
    if (confirmPasswordInput.value &&
        passwordInput.value !== confirmPasswordInput.value) {
      confirmPasswordInput.setCustomValidity("Passwords do not match.");
    } else {
      confirmPasswordInput.setCustomValidity("");
    }
  }

  passwordInput.addEventListener("input", validatePasswords);
  confirmPasswordInput.addEventListener("input", validatePasswords);

  registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    validatePasswords();
    if (!registerForm.reportValidity()) return;

    setSubmitting(registerForm, true);
    showMessage(registerMessage, "Creating your account…");

    try {
      const result = await window.SyndeoAPI.register({
        displayName: registerForm.elements.fullName.value.trim(),
        email: registerForm.elements.email.value.trim(),
        password: passwordInput.value,
        accountType: registerForm.elements.accountType.value
      });

      showMessage(
        registerMessage,
        result.message || "Account created successfully. Please sign in."
      );

      registerForm.reset();

      window.setTimeout(() => {
        window.location.href = "login.html";
      }, 1200);
    } catch (error) {
      showMessage(
        registerMessage,
        error.message || "Registration failed. Please try again.",
        true
      );
    } finally {
      setSubmitting(registerForm, false);
    }
  });
}
