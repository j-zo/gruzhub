"use client";

import { Button } from "@mantine/core";
import Image from "next/image";
import Link from "next/link";

interface Props {
  isSidebar: boolean;
  isShowLogin?: boolean;
  openSidebar?(): void;

  isMdPadding: boolean;
}

export const Navbar = ({
  isSidebar,
  isShowLogin,
  openSidebar,
  isMdPadding,
}: Props): JSX.Element => {
  return (
    <div
      className={`h-14 xl:h-20 border-b-2 flex flex-row items-center ${
        isMdPadding ? "xl:px-10" : ""
      }`}
    >
      <Link href="https://gruzhub.ru" className="flex items-center">
        <div className="relative w-8 h-8 xl:w-12 xl:h-12">
          <Image src="/logo.svg" fill alt="Logo" />
        </div>

        <div className="font-bold ml-2 text-md xl:text-xl">ГрузХаб</div>
      </Link>

      <div className="ml-auto text-sm hidden xl:block">
        По всем вопросам (WhatsApp):{" "}
        <a href="tel:+79950084323" target="_blank">
          +7 (995) 008-43-23
        </a>
      </div>

      {isShowLogin && (
        <Button
          className="ml-auto xl:ml-5"
          onClick={() => {
            window.location.href = "/";
          }}
        >
          <div className="px-5">Вход</div>
        </Button>
      )}

      {isSidebar && (
        <div
          className="xl:hidden ml-auto relative h-8 w-8 cursor-pointer"
          onClick={() => {
            if (openSidebar) {
              openSidebar();
            }
          }}
        >
          <Image src="/burger.svg" fill alt="Open menu" />
        </div>
      )}
    </div>
  );
};
