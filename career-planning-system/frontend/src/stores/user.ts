import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'));
  const userInfo = ref<any>(null);
  const isLoggedIn = ref(!!localStorage.getItem('token'));

  function setToken(newToken: string) {
    token.value = newToken;
    localStorage.setItem('token', newToken);
    isLoggedIn.value = true;
  }

  function setUserInfo(user: any) {
    userInfo.value = user;
    localStorage.setItem('user', JSON.stringify(user));
  }

  function login(data: any) {
    // data: { token, username, id, name, gender, email }
    setToken(data.token);
    setUserInfo(data);
  }

  function logout() {
    token.value = null;
    userInfo.value = null;
    isLoggedIn.value = false;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  function init() {
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');
    if (storedToken) {
      token.value = storedToken;
      isLoggedIn.value = true;
    }
    if (storedUser) {
      try {
        userInfo.value = JSON.parse(storedUser);
      } catch {
        userInfo.value = null;
      }
    }
  }

  return { token, userInfo, isLoggedIn, setToken, setUserInfo, login, logout, init };
});
