import React, { useEffect, useState } from "react";
import FormValidator from "../../../util/FormValidator";
import UserApiRepository from "../data/UserApiRepository";
import LoadingComponent from "../../../util/components/LoadingComponent";
import { UserRole } from "../domain/UserRole";
import {
  Button,
  Checkbox,
  NumberInput,
  Select,
  TextInput,
} from "@mantine/core";
import { PhoneInput } from "../../../util/components/PhoneInput";
import StringUtils from "../../../util/StringUtils";
import { useRegions } from "../../regions/hooks/useRegions";
import { useCountries } from "../../regions/hooks/useCountries";
import { useTelegramUserId } from "../hooks/useTelegramUserId";

interface Props {
  userApiRepository: UserApiRepository;

  onSignIn(): void;
  userRole: UserRole;
}

export const SignUpComponent = ({
  userApiRepository,

  onSignIn,
  userRole,
}: Props): JSX.Element => {
  const regions = useRegions(/* isOnlyRussia */ false);
  const countries = useCountries();

  const [name, setName] = useState("");
  const [inn, setInn] = useState("");

  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("7");

  const [countryCode, setCountryCode] = useState("RU");
  const [regionId, setRegionId] = useState<number | undefined>();
  const [city, setCity] = useState("");
  const [street, setStreet] = useState("");

  const [isMobileTeam, setMobileTeam] = useState(false);
  const [tripRadiusKm, setTripRadiusKm] = useState(0);

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [isLoading, setLoading] = useState(false);

  const [nameError, setNameError] = useState(false);
  const [innError, setInnError] = useState(false);

  const [regionError, setRegionError] = useState(false);
  const [cityError, setCityError] = useState(false);
  const [streetError, setStreetError] = useState(false);

  const [emailError, setEmailError] = useState(false);
  const [phoneError, setPhoneError] = useState(false);

  const [passwordError, setPasswordError] = useState(false);
  const [confirmPasswordError, setConfirmPasswordError] = useState(false);

  const [signUpError, setSignUpError] = useState("");

  const userTelegramId = useTelegramUserId();

  const validateFieldsForSignUp = (): boolean => {
    if (!name) {
      setNameError(true);
      return false;
    }

    if (inn && (`${inn}`.length > 12 || `${inn}`.length < 10)) {
      setInnError(true);
      return false;
    }

    if (!regionId) {
      setRegionError(true);
      return false;
    }

    if (!email) {
      setEmailError(true);
      return false;
    } else if (!FormValidator.isValidEmail(email)) {
      setEmailError(true);
      return false;
    }

    if (!phone) {
      setPhoneError(true);
      return false;
    } else if (phone.length < 11) {
      setPhoneError(true);
      return false;
    }

    if (!password) {
      setPasswordError(true);
      return false;
    } else {
      setPasswordError(false);
    }

    if (!confirmPassword) {
      setConfirmPasswordError(true);
      return false;
    } else if (password !== confirmPassword) {
      setConfirmPasswordError(true);
      return false;
    } else {
      setConfirmPasswordError(false);
    }

    return true;
  };

  const onSignUp = async () => {
    setSignUpError("");

    if (validateFieldsForSignUp()) {
      if (!regionId) throw new Error("NPE");
      setLoading(true);

      try {
        await userApiRepository.signUp({
          name,
          inn,
          role: userRole,

          email,
          phone: StringUtils.cleanPhoneFormatting(phone),
          password: password,

          tripRadiusKm: tripRadiusKm,

          regionId,
          city,
          street,
        });
        await userApiRepository.signIn(
          email,
          StringUtils.cleanPhoneFormatting(phone),
          password,
          userRole
        );

        if (userTelegramId) {
          await userApiRepository.connectTelegramViaWebapp(userTelegramId);
        }
      } catch (e) {
        setSignUpError((e as Error).message);
      }
    }

    setLoading(false);
  };

  return (
    <>
      <TextInput
        label="Название компании"
        placeholder='ООО "Грузовые машины"'
        value={name}
        error={nameError}
        onChange={(e) => {
          setNameError(false);
          setName(e.currentTarget.value);
        }}
        autoComplete="off"
      />

      <TextInput
        label="ИНН"
        placeholder="772842103808"
        value={inn}
        error={innError}
        onChange={(e) => {
          setInnError(false);
          setInn(e.currentTarget.value.replace(/[^0-9]/g, "").slice(0, 12));
        }}
        autoComplete="off"
      />

      {countries.length === 0 || regions.length === 0 ? (
        <div className="flex justify-center mt-4 mb-1">
          <LoadingComponent size="xs" />
        </div>
      ) : (
        <>
          {userRole === UserRole.CUSTOMER && (
            <Select
              label="Страна"
              placeholder="Выберите страну"
              data={countries.map((country) => {
                return {
                  label: country.name,
                  value: country.code,
                };
              })}
              searchable
              value={countryCode}
              onChange={(value) => {
                setCountryCode(value || "RU");
                setRegionId(
                  regions.find((region) => region.country.code === value)?.id
                );
              }}
              autoComplete="off"
            />
          )}

          <Select
            label="Регион"
            placeholder="Ленинградская область"
            data={regions
              .filter((region) => region.country.code === countryCode)
              .map((region) => region.name)}
            searchable
            value={regions.find((region) => region.id === regionId)?.name}
            error={regionError}
            onChange={(value) => {
              setRegionError(false);
              setRegionId(regions.find((region) => region.name === value)?.id);
            }}
            autoComplete="off"
          />
        </>
      )}

      {userRole === UserRole.MASTER && (
        <>
          <TextInput
            label="Город"
            placeholder="Санкт-Петербург"
            value={city}
            error={cityError}
            onChange={(e) => {
              setCityError(false);
              setCity(e.currentTarget.value);
            }}
            autoComplete="off"
          />

          <TextInput
            label="Улица"
            placeholder="Невский проспект 15"
            value={street}
            error={streetError}
            onChange={(e) => {
              setStreetError(false);
              setStreet(e.currentTarget.value);
            }}
            autoComplete="off"
          />
        </>
      )}

      {userRole === UserRole.MASTER && (
        <>
          <div className="mt-4">
            <Checkbox
              checked={isMobileTeam}
              onChange={() => {
                const isMobileTeamNewValue = !isMobileTeam;

                setMobileTeam(isMobileTeamNewValue);
                if (!isMobileTeamNewValue) {
                  setTripRadiusKm(0);
                }
              }}
              label="Есть выездная бригада"
            />
          </div>

          <div className="mt-1" />

          {isMobileTeam && (
            <NumberInput
              label="Радиус выезда в км"
              value={tripRadiusKm}
              onChange={(value) => setTripRadiusKm(+value)}
              min={0}
              step={1}
              autoComplete="off"
            />
          )}
        </>
      )}

      <div className="mt-7" />

      <TextInput
        label="Почта"
        placeholder="your@email.com"
        value={email}
        onChange={(e) => {
          setEmailError(false);
          setEmail(e.currentTarget.value.trim().toLowerCase());
        }}
        error={emailError}
        type="email"
        autoComplete="off"
      />

      <PhoneInput
        label="Номер телефона"
        phone={phone}
        error={phoneError}
        onChange={(value) => {
          setPhone(value);
          setPhoneError(false);
        }}
      />

      <TextInput
        label="Пароль"
        placeholder="Введите пароль"
        value={password}
        error={passwordError}
        onChange={(e) => {
          setPasswordError(false);
          setPassword(e.currentTarget.value);
        }}
        type="password"
        autoComplete="off"
      />

      <TextInput
        label="Подтвердите пароль"
        placeholder="Повторите пароль"
        value={confirmPassword}
        onChange={(e) => {
          setConfirmPassword(e.currentTarget.value);
          setConfirmPasswordError(false);
        }}
        error={confirmPasswordError}
        type="password"
        autoComplete="off"
      />

      <div className="mt-7" />

      <Button disabled={isLoading} fullWidth onClick={() => onSignUp()}>
        Зарегистрироваться{" "}
        {userRole === UserRole.CUSTOMER
          ? "как перевозчик"
          : userRole === UserRole.MASTER
          ? "как автосервис"
          : ""}
      </Button>

      {signUpError && (
        <div className="mt-3 flex justify-center text-red-500 text-sm">
          {signUpError}
        </div>
      )}

      <div className="flex justify-center text-sm mt-3">
        Уже есть аккаунт?&nbsp;
        <div
          className="t text-blue-700 cursor-pointer"
          onClick={() => onSignIn()}
        >
          Войти
        </div>
      </div>

      <div className="flex justify-center text-sm mt-1">
        <a href="/privacy">Политика конфиденциальности</a>
      </div>
    </>
  );
};
