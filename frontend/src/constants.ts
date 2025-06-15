enum Pages {
  HOME_PAGE = "/",
  // auth
  PASSWORD_RESET = "/password-reset",
  CHANGE_PASSWORD = "/change-password",
  // admin
  USERS = "/users",
}

const isProdForDev = false;

let APPLICATION_SERVER = process.env.NEXT_PUBLIC_APPLICATION_SERVER || "";
let SSR_APPLICATION_SERVER =
  process.env.NEXT_PUBLIC_SSR_APPLICATION_SERVER || "";

if (isProdForDev) {
  APPLICATION_SERVER = "https://app.gruzhub.ru";
  SSR_APPLICATION_SERVER = "https://app.gruzhub.ru";
}

// authorization
const AUTHORIED_USER_TOKEN_KEY =
  process.env.NEXT_PUBLIC_AUTHORIED_USER_TOKEN_KEY || "";
const AUTHORIED_USER_ID_KEY =
  process.env.NEXT_PUBLIC_AUTHORIED_USER_ID_KEY || "";
if (!AUTHORIED_USER_TOKEN_KEY)
  throw new Error("NEXT_PUBLIC_AUTHORIED_USER_TOKEN_KEY is not defined");
if (!AUTHORIED_USER_ID_KEY)
  throw new Error("NEXT_PUBLIC_AUTHORIED_USER_ID_KEY is not defined");

const ORDER_PRICE = 1_000;

export {
  Pages,
  APPLICATION_SERVER,
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY,
  SSR_APPLICATION_SERVER,
  ORDER_PRICE,
};
