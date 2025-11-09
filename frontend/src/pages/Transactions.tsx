import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { bankApi, AccountDto, TransactionDto } from '../lib/api/bank';
import { TransactionItem } from '../components/TransactionItem';
import { ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ThemeToggle } from '../components/ThemeToggle';

// Адаптер для преобразования TransactionDto в Transaction (для совместимости с TransactionItem)
interface Transaction {
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

function transactionDtoToTransaction(
  transaction: TransactionDto,
  accountId: string
): Transaction {
  return {
    id: transaction.id,
    user_id: accountId, // Временное значение
    bank_id: accountId,
    transaction_type: transaction.debit_credit_indicator === 'DEBIT' ? 'purchase' : 'cash-in',
    amount: Number(transaction.amount),
    description: transaction.description || transaction.merchant_name || 'Транзакция',
    status: transaction.status?.toLowerCase() || 'confirmed',
    transaction_date: transaction.booking_datetime,
    created_at: transaction.created_at,
  };
}

export const Transactions = () => {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState<AccountDto[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [bankFilter, setBankFilter] = useState<string>('all');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [search, setSearch] = useState<string>('');

  useEffect(() => {
    if (user) {
      loadData();
    }
  }, [user]);

  const loadData = async () => {
    if (!user) return;

    setLoading(true);
    setError(null);

    try {
      // Загружаем все счета
      const accountsData = await bankApi.getAccounts();
      setAccounts(accountsData);

      // Загружаем транзакции для всех счетов
      const allTransactions: Transaction[] = [];

      for (const account of accountsData) {
        try {
          const transactionsPage = await bankApi.getTransactions(account.id, {
            page: 0,
            size: 100, // Загружаем первые 100 транзакций для каждого счета
          });

          const accountTransactions = transactionsPage.content.map((t) =>
            transactionDtoToTransaction(t, account.id)
          );
          allTransactions.push(...accountTransactions);
        } catch (err) {
          console.error(`Error loading transactions for account ${account.id}:`, err);
        }
      }

      // Сортируем по дате (новые сначала)
      allTransactions.sort(
        (a, b) => new Date(b.transaction_date).getTime() - new Date(a.transaction_date).getTime()
      );

      setTransactions(allTransactions);
    } catch (err) {
      console.error('Error loading data:', err);
      setError('Не удалось загрузить транзакции');
    } finally {
      setLoading(false);
    }
  };

  const getBankName = (accountId: string) => {
    const account = accounts.find((a) => a.id === accountId);
    return account ? account.bank_id.toUpperCase() : '';
  };

  const filtered = useMemo(() => {
    return transactions.filter((t) => {
      const bankOk = bankFilter === 'all' || t.bank_id === bankFilter;
      const typeOk =
        typeFilter === 'all' ||
        (typeFilter === 'debit' && t.transaction_type === 'purchase') ||
        (typeFilter === 'credit' && t.transaction_type === 'cash-in');
      const statusOk = statusFilter === 'all' || t.status === statusFilter;
      const searchOk =
        !search ||
        t.description.toLowerCase().includes(search.toLowerCase()) ||
        t.amount.toString().includes(search);
      return bankOk && typeOk && statusOk && searchOk;
    });
  }, [transactions, bankFilter, typeFilter, statusFilter, search]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-purple-50 dark:from-gray-900 dark:to-gray-800 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 dark:border-blue-400 mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-300">Загрузка...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-purple-50 dark:from-gray-900 dark:to-gray-800">
      <div className="max-w-6xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <Link
              to="/"
              className="flex items-center gap-2 px-3 py-2 bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-500 dark:to-purple-500 rounded-lg shadow text-white hover:from-blue-700 hover:to-purple-700 transition hover:shadow-md"
            >
              <ArrowLeft className="w-4 h-4" />
              Назад
            </Link>
            <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent">
              Все операции
            </h1>
          </div>
          <ThemeToggle />
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded-lg mb-4">
            {error}
          </div>
        )}

        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow p-4 mb-6 border border-transparent hover:border-blue-500/20 dark:hover:border-purple-500/20 transition-colors">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Поиск по описанию..."
              className="px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
            />
            <select
              value={bankFilter}
              onChange={(e) => setBankFilter(e.target.value)}
              className="px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
            >
              <option value="all">Все счета</option>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.bank_id.toUpperCase()} - {a.account_number_masked}
                </option>
              ))}
            </select>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
            >
              <option value="all">Все типы</option>
              <option value="credit">Пополнение</option>
              <option value="debit">Списание</option>
            </select>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
            >
              <option value="all">Все статусы</option>
              <option value="confirmed">Подтверждено</option>
              <option value="pending">В ожидании</option>
              <option value="cancelled">Отменено</option>
            </select>
          </div>
        </div>

        <div className="space-y-3">
          {filtered.length > 0 ? (
            filtered.map((t) => (
              <TransactionItem
                key={t.id}
                transaction={t}
                bankName={getBankName(t.bank_id)}
              />
            ))
          ) : (
            <div className="bg-white dark:bg-gray-800 rounded-xl p-8 text-center">
              <p className="text-gray-500 dark:text-gray-400">
                {transactions.length === 0
                  ? 'Нет транзакций. Подключите банк для просмотра операций.'
                  : 'Операции не найдены'}
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
