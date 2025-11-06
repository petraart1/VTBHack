export interface Bank {
  id: string;
  user_id: string;
  bank_name: string;
  card_number: string;
  card_type: string;
  balance: number;
  currency: string;
  color: string;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface Transaction {
  id: string;
  user_id: string;
  bank_id: string;
  transaction_type: string;
  amount: number;
  description: string;
  status: string;
  transaction_date: string;
  created_at: string;
}

export interface Profile {
  id: string;
  full_name: string;
  email: string;
  avatar_url?: string;
  created_at: string;
  updated_at: string;
}
