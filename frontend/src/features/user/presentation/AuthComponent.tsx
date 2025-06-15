import { useState } from "react";
import { SignInComponent } from "./SignInComponent";
import UserApiRepository from "../data/UserApiRepository";
import { SignUpComponent } from "./SignUpComponent";
import { UserRole } from "../domain/UserRole";
import { Navbar } from "../../navbar/Navbar";
import { AuthRoleSelector } from "./AuthRoleSelector";

interface Props {
  userApiRepository: UserApiRepository;
}

export const AuthComponent = ({ userApiRepository }: Props): JSX.Element => {
  const [isShowSignIn, setShowSignIn] = useState(false);
  const [userRole, setUserRole] = useState(UserRole.MASTER);

  return (
    <>
      <Navbar isMdPadding isSidebar={false} />

      <div className="max-w-xs mx-auto my-20">
        <div className="flex justify-center">
          <div className="text-xl font-bold">
            {isShowSignIn ? "Вход" : "Регистрация"}
          </div>
        </div>

        <div
          className="flex justify-center text-sm text-blue-700 cursor-pointer mb-4"
          onClick={() => setShowSignIn(!isShowSignIn)}
        >
          {isShowSignIn ? "Регистрация" : "Вход"}
        </div>

        <AuthRoleSelector userRole={userRole} setUserRole={setUserRole} />

        <div>
          {isShowSignIn ? (
            <SignInComponent
              userApiRepository={userApiRepository}
              onSignUp={() => setShowSignIn(false)}
              userRole={userRole}
            />
          ) : (
            <SignUpComponent
              userApiRepository={userApiRepository}
              onSignIn={() => setShowSignIn(true)}
              userRole={userRole}
            />
          )}
        </div>
      </div>
    </>
  );
};
