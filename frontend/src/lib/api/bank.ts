// API для работы с банковскими данными

import { apiClient } from '../api';

export interface AccountDto {
  id: string;
  bank_id: string;
  external_account_id: string;
  account_number_masked: string;
  iban: string;
  account_type: string;
  currency: string;
  nickname: string | null;
  available_balance: number;
  booked_balance: number;
  credit_limit: number | null;
  status: string;
  last_sync_at: string | null;
  created_at: string;
}

export interface TransactionDto {
  id: string;
  account_id: string;
  external_transaction_id: string;
  booking_datetime: string;
  value_datetime: string;
  amount: number;
  currency: string;
  debit_credit_indicator: 'DEBIT' | 'CREDIT';
  status: string;
  description: string | null;
  merchant_name: string | null;
  merchant_category_code: string | null;
  running_balance: number | null;
  created_at: string;
}

export interface PageDto<T> {
  content: T[];
  total_elements: number;
  page_number: number;
  page_size: number;
  total_pages: number;
  has_next: boolean;
  has_previous: boolean;
}

export interface TransactionFilter {
  fromBookingDateTime?: string;
  toBookingDateTime?: string;
  merchantName?: string;
  merchantCategoryCode?: string;
  debitCreditIndicator?: 'DEBIT' | 'CREDIT';
  page?: number;
  size?: number;
}

export interface BankInfo {
  id: string;
  name: string;
}

export const bankApi = {
  // Получить список доступных банков
  // nginx: /api/v1/bank/ -> bank_service/
  // контроллер: @RequestMapping("/api/v1/bank/banks")
  // запрос /api/v1/bank/api/v1/bank/banks -> bank_service/api/v1/bank/banks ✓
  async getAvailableBanks(): Promise<BankInfo[]> {
    return apiClient.get<BankInfo[]>('/api/v1/bank/api/v1/bank/banks');
  },

  // Получить список счетов пользователя
  async getAccounts(bankId?: string): Promise<AccountDto[]> {
    const query = bankId ? `?bankId=${bankId}` : '';
    return apiClient.get<AccountDto[]>(`/api/v1/bank/api/v1/bank/accounts${query}`);
  },

  // Получить детали счета
  async getAccountDetails(accountId: string): Promise<AccountDto> {
    return apiClient.get<AccountDto>(`/api/v1/bank/api/v1/bank/accounts/${accountId}`);
  },

  // Получить транзакции по счету
  async getTransactions(
    accountId: string,
    filter?: TransactionFilter
  ): Promise<PageDto<TransactionDto>> {
    const params = new URLSearchParams();
    
    if (filter?.fromBookingDateTime) {
      params.append('fromBookingDateTime', filter.fromBookingDateTime);
    }
    if (filter?.toBookingDateTime) {
      params.append('toBookingDateTime', filter.toBookingDateTime);
    }
    if (filter?.merchantName) {
      params.append('merchantName', filter.merchantName);
    }
    if (filter?.merchantCategoryCode) {
      params.append('merchantCategoryCode', filter.merchantCategoryCode);
    }
    if (filter?.debitCreditIndicator) {
      params.append('debitCreditIndicator', filter.debitCreditIndicator);
    }
    if (filter?.page !== undefined) {
      params.append('page', filter.page.toString());
    }
    if (filter?.size !== undefined) {
      params.append('size', filter.size.toString());
    }

    const query = params.toString();
    const url = `/api/v1/bank/api/v1/bank/accounts/${accountId}/transactions${query ? `?${query}` : ''}`;
    
    return apiClient.get<PageDto<TransactionDto>>(url);
  },

  // Получить детали транзакции
  async getTransactionDetails(
    accountId: string,
    transactionId: string
  ): Promise<TransactionDto> {
    return apiClient.get<TransactionDto>(
      `/api/v1/bank/api/v1/bank/accounts/${accountId}/transactions/${transactionId}`
    );
  },
};

