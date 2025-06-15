"use client";
import React, { useEffect } from "react";
import { MenuTab } from "../domain/MenuTab";
import Link from "next/link";
import Image from "next/image";

interface Props {
  tabs: MenuTab[];
  selectedTab?: MenuTab;
  closeSidebar(): void;
}

export const Sidebar = ({
  tabs,
  selectedTab,
  closeSidebar,
}: Props): JSX.Element => {
  useEffect(() => {
    document.body.style.overflowY = "hidden";
    return () => {
      document.body.style.overflowY = "auto";
    };
  }, []);

  return (
    <div className="min-w-full min-h-screen fixed top-0 bg-white left-0 z-50 p-5">
      <div className="h-8 flex flex-row items-center mb-4">
        <div className="text-xl font-bold pl-3">Меню</div>

        <div
          className="relative h-8 w-8 ml-auto cursor-pointer"
          onClick={() => closeSidebar()}
        >
          <Image src="/close.svg" fill alt="close" />
        </div>
      </div>

      <div className="my-7 text-sm mx-1 text-center">
        По всем вопросам{" "}
        <Link className="text-blue-700" href="tel:+79950084323">
          +7 (995) 008-43-23
        </Link>{" "}
        (WhatsApp)
      </div>

      {tabs.map((tab) => {
        const isSelected = tab.url == selectedTab?.url;

        return (
          <Link
            key={`tab_${tab.url}`}
            className={`${
              isSelected ? "!bg-blue-700 text-white" : ""
            } hover:bg-blue-100 cursor-pointer py-2 px-3 mb-2 rounded-lg text-lg block`}
            href={tab.url}
          >
            {tab.name}
          </Link>
        );
      })}
    </div>
  );
};
