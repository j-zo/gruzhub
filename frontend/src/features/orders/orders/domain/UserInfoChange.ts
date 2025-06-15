export interface UserInfoChange {
  id: number;
  userId: number;

  previousName?: string;
  newName?: string;

  previousPhone?: string;
  newPhone?: string;

  previousEmail?: string;
  newEmail?: string;

  previousInn?: string;
  newInn?: string;

  date: number;
}
