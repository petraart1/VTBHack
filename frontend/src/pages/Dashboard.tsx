import { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { bankApi, AccountDto } from '../lib/api/bank';
import { BankCard } from '../components/BankCard';
import { LogOut, Wallet, List } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { ThemeToggle } from '../components/ThemeToggle';

// Адаптер для преобразования AccountDto в Bank (для совместимости с BankCard)
interface Bank {
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

function accountToBank(account: AccountDto): Bank {
  // Маппинг цветов для разных банков
  const bankColors: Record<string, string> = {
    vbank: 'blue',
    abank: 'purple',
    sbank: 'green',
  };

  return {
    id: account.id,
    user_id: account.id, // Временное значение
    bank_name: account.bank_id.toUpperCase(),
    card_number: account.account_number_masked || '**** **** **** ****',
    card_type: account.account_type || 'CARD',
    balance: Number(account.available_balance || account.booked_balance || 0),
    currency: account.currency || 'RUB',
    color: bankColors[account.bank_id.toLowerCase()] || 'gradient',
    is_active: account.status === 'ACTIVE',
    created_at: account.created_at,
    updated_at: account.last_sync_at || account.created_at,
  };
}

export const Dashboard = () => {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [banks, setBanks] = useState<Bank[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      loadData();
    } else {
      setLoading(false);
    }
  }, [user]);

  const loadData = async () => {
    if (!user) return;

    try {
      setError(null);
      const accounts = await bankApi.getAccounts();
      const banksData = accounts.map(accountToBank);
      setBanks(banksData);
    } catch (error) {
      console.error('Error loading accounts:', error);
      setError('Не удалось загрузить счета. Убедитесь, что вы подключили банк.');
    } finally {
      setLoading(false);
    }
  };

  const getTotalBalance = () => {
    return banks.reduce((total, bank) => total + bank.balance, 0);
  };

  const handleCardClick = (bank: Bank) => {
    navigate('/transfer', { state: { bankId: bank.id } });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-purple-50 dark:from-gray-900 dark:to-gray-800 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-300">Загрузка...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-purple-50 dark:from-gray-900 dark:to-gray-800">
      <div className="max-w-6xl mx-auto px-4 py-8">
        <div className="flex justify-between items-center mb-8">
          <div className="grid items-center grid-cols-[auto_1fr] gap-4">
            <div className="h-16 w-16 rounded-full bg-gradient-to-r from-blue-600 to-purple-600 text-white flex items-center justify-center font-semibold text-2xl dark:from-blue-500 dark:to-purple-500">
              {((user?.firstName || user?.email || 'U') as string).charAt(0).toUpperCase()}
            </div>
            <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent">
              Мультибанк
            </h1>
            <div></div>
            <p className="text-gray-600 dark:text-gray-300 mt-[-23px]">
              {user?.firstName && user?.lastName
                ? `${user.firstName} ${user.lastName}`
                : user?.email}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <ThemeToggle />
            <Link
              to="/transactions"
              className="flex items-center gap-2 px-3 py-2 bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-500 dark:to-purple-500 rounded-lg shadow text-white hover:from-blue-700 hover:to-purple-700 transition hover:shadow-md"
            >
              <List className="w-4 h-4" />
              Все операции
            </Link>
            <button
              onClick={() => signOut()}
              className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-500 dark:to-purple-500 rounded-lg shadow text-white hover:from-blue-700 hover:to-purple-700 transition hover:shadow-md"
            >
              <LogOut className="w-4 h-4" />
              Выйти
            </button>
          </div>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-lg p-8 mb-8 border border-transparent hover:border-blue-500/20 dark:hover:border-purple-500/20 transition-colors">
          <div className="flex items-center gap-3 mb-4">
            <div className="bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-500 dark:to-purple-500 p-3 rounded-full">
              <Wallet className="w-6 h-6 text-white" />
            </div>
            <div>
              <p className="text-sm text-gray-600 dark:text-gray-400">Общий баланс</p>
              <p className="text-4xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent">
                {getTotalBalance().toLocaleString('ru-RU', {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}{' '}
                ₽
              </p>
            </div>
          </div>
        </div>

        {error && (
          <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 text-yellow-700 dark:text-yellow-400 px-4 py-3 rounded-lg mb-4">
            {error}
          </div>
        )}

        <div className="mb-8">
          <h2 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent mb-4">
            Ваши счета
          </h2>
          {banks.length === 0 ? (
            <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-lg p-8 text-center">
              <p className="text-gray-600 dark:text-gray-300 mb-4">
                У вас пока нет подключенных счетов
              </p>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Подключите банк через API для просмотра счетов и транзакций
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {banks.map((bank) => (
                <div key={bank.id} onClick={() => handleCardClick(bank)} className="cursor-pointer">
                  <BankCard bank={bank} />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
