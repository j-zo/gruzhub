"use client";

import {
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY,
} from "../../constants";
import { Navbar } from "../../features/navbar/Navbar";
import UserApiRepository from "../../features/user/data/UserApiRepository";
import ApiHelper from "../../util/api/ApiHelper";
import { ChangePasswordComponent } from "../../features/user/presentation/ChangePasswordComponent";

const apiHelper = new ApiHelper();
const userApiRepository = new UserApiRepository(
  apiHelper,
  AUTHORIED_USER_TOKEN_KEY,
  AUTHORIED_USER_ID_KEY
);
apiHelper.attachUserRepository(userApiRepository);

export default function Page() {
  return (
    <>
      <Navbar isMdPadding={false} isSidebar={false} />

      <div className="max-w-xs mx-auto mt-20">
        <ChangePasswordComponent userApiRepository={userApiRepository} />
      </div>
    </>
  );
}
