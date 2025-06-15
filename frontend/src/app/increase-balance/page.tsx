import Head from "next/head";
import Link from "next/link";
import { Navbar } from "../../features/navbar/Navbar";

export default function Page() {
  return (
    <>
      <Head>
        <meta name="robots" content="noindex" />
      </Head>

      <div>
        <Navbar isMdPadding={false} isSidebar={false} isShowLogin />

        <h1 className="font-bold text-2xl my-5">Пополнение баланса</h1>

        <div className="mt-5">
          Чтобы пополнить баланс, напишите или позвоните нашему менеджеру.
          <br />
          <br />
          Баланс можно пополнить:
          <br />
          - переводом на карту;
          <br />
          - наличным средствами;
          <br />- оплатой по счёту.
          <br />
          <br />
          <b>Контакты менеджеров:</b>
          <br />
          <Link href="tel:+79950084323" target="_blank">
            +7 (995) 008-43-23
          </Link>{" "}
          - телефон \ WhatsApp (Андрей)
          <br />
          <Link href="https://t.me/rostislav_dugin" target="_blank">
            @rostislav_dugin
          </Link>{" "}
          - Telegram (Ростислав)
          <br />
          <Link href="mailto:rostislav.dugin@gmail.com" target="_blank">
            rostislav.dugin@gmail.com
          </Link>{" "}
          - почта (Ростислав)
        </div>
      </div>
    </>
  );
}
