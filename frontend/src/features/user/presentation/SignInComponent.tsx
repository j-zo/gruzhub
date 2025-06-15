import Link from "next/link";
import React, { useState } from "react";
import UserApiRepository from "../data/UserApiRepository";
import { Pages } from "../../../constants";
import { Button, TextInput } from "@mantine/core";
import StringUtils from "../../../util/StringUtils";
import { UserRole } from "../domain/UserRole";
import { useTelegramUserId } from "../hooks/useTelegramUserId";

interface Props {
  userApiRepository: UserApiRepository;
  onSignUp(): void;
  userRole: UserRole;
}

const SignInComponent = ({
  userApiRepository,
  onSignUp,
  userRole,
}: Props): JSX.Element => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setLoading] = useState(false);

  const [usernameError, setUsernameError] = useState(false);
  const [passwordError, setPasswordError] = useState(false);
  const [signInError, setSignInError] = useState("");

  const userTelegramId = useTelegramUserId();

  const validateFieldsForSignIn = (): boolean => {
    let isValid = true;

    if (!username) {
      setUsernameError(true);
      isValid = false;
    } else {
      setUsernameError(false);
    }

    if (!password) {
      setPasswordError(true);
      isValid = false;
    } else {
      setPasswordError(false);
    }

    return isValid;
  };

  const onSignIn = async () => {
    setSignInError("");
    setLoading(true);

    if (validateFieldsForSignIn()) {
      try {
        await userApiRepository.signIn(
          /* email */ StringUtils.isPhone(username) ? undefined : username,
          /* phone */ StringUtils.isPhone(username)
            ? StringUtils.cleanPhoneFormatting(username)
            : undefined,
          password,
          userRole
        );

        if (userTelegramId) {
          await userApiRepository.connectTelegramViaWebapp(userTelegramId);
        }
      } catch (e) {
        setSignInError((e as Error).message);
      }
    }

    setLoading(false);
  };

  const isPhone = StringUtils.isPhone(username);

  return (
    <>
      <TextInput
        label="Почта или номер телефона"
        placeholder="Введите почту или номер"
        description={isPhone ? "Вводите номер телефона, начиная с +7" : ""}
        value={username}
        error={usernameError}
        onChange={(e) => {
          setUsernameError(false);
          setUsername(e.currentTarget.value.trim().toLowerCase());
        }}
        autoComplete="off"
      />

      <TextInput
        label="Пароль"
        placeholder="Введите пароль"
        value={password}
        onChange={(e) => {
          setPassword(e.currentTarget.value);
          setPasswordError(false);
        }}
        error={passwordError}
        type="password"
        autoComplete="off"
      />

      <div className="mt-3" />

      <Button disabled={isLoading} fullWidth onClick={() => onSignIn()}>
        Войти{" "}
        {userRole === UserRole.CUSTOMER
          ? "как перевозчик"
          : userRole === UserRole.MASTER
          ? "как автосервис"
          : ""}
      </Button>

      {signInError && (
        <div className="mt-3 text-center flex justify-center text-red-500 text-sm">
          {signInError}
        </div>
      )}

      <div className="flex justify-center text-sm mt-3">
        Ещё нет аккаунта?&nbsp;
        <div
          className="text-blue-700 cursor-pointer"
          onClick={() => onSignUp()}
        >
          Регистрация
        </div>
      </div>

      <div className="flex justify-center text-sm mt-1">
        <Link
          className="text-blue-700 cursor-pointer"
          href={Pages.PASSWORD_RESET}
        >
          Восстановление пароля
        </Link>
      </div>
    </>
  );
};

export { SignInComponent };
