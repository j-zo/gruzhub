export default class StringUtils {
  static cutBySymbolsLength = (string?: string, length?: number): string => {
    if (string && length) {
      return string.length > length
        ? `${string.substring(0, length)}...`
        : string.substring(0, length);
    }
    return "";
  };

  /**
   * Coverts number into number with two digits after dot.
   *
   * For example:
   *  10    => 10.00
   *  10.1  => 10.10
   *  10.11 => 10.11
   */
  static getNumberWithHundredsZeros(number: number): string {
    const [beforeDot, afterDot] = `${number}`.split(".");

    if (!afterDot) {
      return `${beforeDot}.00`;
    }

    if (afterDot.length === 1) {
      return `${number}0`;
    }

    if (afterDot.length > 2) {
      return `${beforeDot}.${afterDot.substr(0, 2)}`;
    }

    return `${number}`;
  }

  static cutNumberToTwoSynbolsAfterDot(number: string): string {
    let numberWithOnlyDots = `${number}`.replace(/,/g, ".");
    if (!`${number}`.includes(".")) return numberWithOnlyDots;
    return number.split(".")[0] + "." + number.split(".")[1].slice(0, 2);
  }

  static formatMoney(money: number) {
    return Number(
      StringUtils.getNumberWithHundredsZeros(money)
    ).toLocaleString();
  }

  static isValidJson(text: string): boolean {
    try {
      JSON.parse(text);
    } catch (e) {
      return false;
    }

    return true;
  }

  static cleanPhoneFormatting(phone: string): string {
    return phone
      .replace(/\)/g, "")
      .replace(/\(/g, "")
      .replace(/ /g, "")
      .replace(/-/g, "")
      .replace(/\+/g, "");
  }

  static isPhone(username: string): boolean {
    const onlyDitisRegex = /^[0-9]*$/;
    const usernameWithoutPhoneFormatting =
      StringUtils.cleanPhoneFormatting(username);
    const isPhone =
      usernameWithoutPhoneFormatting.length > 0 &&
      onlyDitisRegex.test(usernameWithoutPhoneFormatting) &&
      usernameWithoutPhoneFormatting.length < 12;

    return isPhone;
  }

  static formatAsPhone(phone?: string): string {
    if (!phone) {
      return "";
    }

    phone = phone.slice(0, 13);

    if (phone.length === 0) return ``;
    if (phone.length === 1) return `+${phone[0]}`;
    if (phone.length === 2) return `+${phone[0]} (${phone[1]}`;
    if (phone.length === 3) return `+${phone[0]} (${phone.slice(1)}`;
    if (phone.length === 4) return `+${phone[0]} (${phone.slice(1)}) `;
    if (phone.length === 5)
      return `+${phone[0]} (${phone.slice(1, 4)}) ${phone.slice(4)}`;
    if (phone.length === 6)
      return `+${phone[0]} (${phone.slice(1, 4)}) ${phone.slice(4)}`;
    if (phone.length === 7)
      return `+${phone[0]} (${phone.slice(1, 4)}) ${phone.slice(4, 7)}-`;
    if (phone.length === 8)
      return `+${phone[0]} (${phone.slice(1, 4)}) ${phone.slice(
        4,
        7
      )}-${phone.slice(7)}`;
    if (phone.length === 9)
      return `+${phone[0]} (${phone.slice(1, 4)}) ${phone.slice(
        4,
        7
      )}-${phone.slice(7, 9)}-`;
    if (phone.length === 10)
      return `+${phone[0]} (${phone.slice(1, 4)}) ${phone.slice(
        4,
        7
      )}-${phone.slice(7, 9)}-${phone.slice(9)}`;
    if (phone.length === 11)
      return `+${phone[0]} (${phone.slice(1, 4)}) ${phone.slice(
        4,
        7
      )}-${phone.slice(7, 9)}-${phone.slice(9)}`;
    if (phone.length === 12)
      return `+${phone.slice(0, 2)} (${phone.slice(2, 5)}) ${phone.slice(
        5,
        8
      )}-${phone.slice(8, 10)}-${phone.slice(10)}`;
    if (phone.length === 13)
      return `+${phone.slice(0, 3)} (${phone.slice(3, 6)}) ${phone.slice(
        6,
        9
      )}-${phone.slice(9, 11)}-${phone.slice(11)}`;

    throw new Error("Unexpected phone input");
  }
}
