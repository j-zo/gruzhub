export class ArrayUtils {
  static isEqualNumberSets(array1: number[], array2: number[]): boolean {
    if (array1.length !== array2.length) return false;

    for (const item of array1) {
      if (!array2.includes(item)) {
        return false;
      }
    }

    return true;
  }
}
