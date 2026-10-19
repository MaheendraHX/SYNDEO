
/* SYNDEO — Authentication Page Behaviour */

const loginForm = document.querySelector("#login-form");
const loginMessage = document.querySelector("#login-message");

const registerForm = document.querySelector("#register-form");
const registerMessage = document.querySelector("#register-message");

document.querySelectorAll("[data-demo-entry]").forEach((link) => {
  link.addEventListener("click", () => {
    try {
      window.localStorage.setItem("syndeo.demo.mode.v1", "true");
    } catch (error) {
      console.error("Could not store the non-sensitive demo-mode flag.", error);
      const message = loginMessage || registerMessage;
      if (message) {
        message.textContent =
          "The demo will open, but this browser could not save the demo-mode preference.";
      }
    }
  });
});

/* Login form */
if (loginForm && loginMessage) {
  loginForm.addEventListener("submit", (event) => {
    event.preventDefault();

    if (!loginForm.reportValidity()) {
      return;
    }

    loginMessage.textContent =
      "Your form is ready. Login will be enabled once the backend is connected.";
  });
}

/* Registration form */
if (registerForm && registerMessage) {
  const passwordInput = document.querySelector("#register-password");
  const confirmPasswordInput = document.querySelector("#confirm-password");

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

  registerForm.addEventListener("submit", (event) => {
    event.preventDefault();

    validatePasswords();

    if (!registerForm.reportValidity()) {
      return;
    }

    registerMessage.textContent =
      "Your form is ready. Account creation will be enabled once the backend is connected.";
  });
}
