import { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { supabase } from '../lib/supabase';
import { Bank } from '../types';
import { BankCard } from '../components/BankCard';
import { LogOut, Wallet, List } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { ThemeToggle } from '../components/ThemeToggle';

export const Dashboard = () => {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [banks, setBanks] = useState<Bank[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, [user]);

  const loadData = async () => {
    if (!user) return;

    try {
      const { data: banksData } = await supabase
        .from('banks')
        .select('*')
        .eq('user_id', user.id)
        .eq('is_active', true)
        .order('created_at', { ascending: true });

      if (banksData && banksData.length === 0) {
        await seedMockData();
      } else {
        setBanks(banksData || []);
      }
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      setLoading(false);
    }
  };

  const seedMockData = async () => {
    if (!user) return;

    const mockBanks = [
      {
        user_id: user.id,
        bank_name: 'Сбербанк',
        card_number: '4532123456789749',
        card_type: 'VISA',
        balance: 15430.50,
        currency: 'RUB',
        color: 'gradient',
      },
      {
        user_id: user.id,
        bank_name: 'ВТБ',
        card_number: '5425233430109903',
        card_type: 'MasterCard',
        balance: 8920.75,
        currency: 'RUB',
        color: 'blue',
      },
      {
        user_id: user.id,
        bank_name: 'Альфа-Банк',
        card_number: '4716347184862961',
        card_type: 'VISA',
        balance: -250.00,
        currency: 'RUB',
        color: 'purple',
      },
    ];

    const { data: insertedBanks } = await supabase
      .from('banks')
      .insert(mockBanks)
      .select();

    if (insertedBanks && insertedBanks.length > 0) {
      // Полный замоканный JSON с данными о транзакциях
      const mockTransactionsData = [
        {
          type: 'cash-in',
          amount: 500.00,
          fromPerson: 'Иван Петров',
          fromBank: 'ABC Банк',
          status: 'confirmed',
          hoursAgo: 2,
        },
        {
          type: 'purchase',
          amount: 175.50,
          store: 'Магазин',
          status: 'confirmed',
          hoursAgo: 5,
        },
        {
          type: 'transfer',
          amount: 9000.00,
          toPerson: 'Мария Сидорова',
          toBank: 'XYZ Кредит',
          status: 'confirmed',
          hoursAgo: 24,
        },
        {
          type: 'transfer',
          amount: 9267.00,
          toPerson: 'Петр Иванов',
          toBank: 'Global Финанс',
          status: 'cancelled',
          hoursAgo: 48,
          reason: 'Недостаточно средств',
        },
        {
          type: 'cash-in',
          amount: 350.00,
          fromPerson: 'Анна Смирнова',
          fromBank: 'Тинькофф',
          status: 'confirmed',
          hoursAgo: 72,
        },
        {
          type: 'cash-out',
          amount: 2500.00,
          fromBank: 'Сбербанк',
          status: 'confirmed',
          hoursAgo: 96,
        },
        {
          type: 'transfer',
          amount: 15000.00,
          toPerson: 'Елена Козлова',
          toBank: 'Альфа-Банк',
          status: 'pending',
          hoursAgo: 120,
        },
        {
          type: 'purchase',
          amount: 999.99,
          store: 'Супермаркет',
          status: 'confirmed',
          hoursAgo: 144,
        },
      ];

      const mockTransactions = mockTransactionsData.map((tx, index) => {
        let description = '';
        if (tx.type === 'cash-in') {
          description = `Пополнение от ${tx.fromPerson}, ${tx.fromBank}`;
        } else if (tx.type === 'transfer') {
          description = tx.reason 
            ? `Перевод ${tx.toPerson}, ${tx.toBank} - ${tx.reason}`
            : `Перевод ${tx.toPerson}, ${tx.toBank}`;
        } else if (tx.type === 'cash-out') {
          description = `Снятие наличных в банкомате ${tx.fromBank}`;
        } else if (tx.type === 'purchase') {
          description = `Кэшбэк за покупку в ${tx.store}`;
        }

        return {
          user_id: user.id,
          bank_id: insertedBanks[index % insertedBanks.length].id,
          transaction_type: tx.type,
          amount: tx.amount,
          description: description,
          status: tx.status,
          transaction_date: new Date(Date.now() - tx.hoursAgo * 60 * 60 * 1000).toISOString(),
        };
      });

      await supabase.from('transactions').insert(mockTransactions);
    }

    await loadData();
  };

  const getTotalBalance = () => {
    return banks.reduce((total, bank) => total + bank.balance, 0);
  };

  const handleCardClick = (bank: Bank) => {
    navigate('/transfer', { state: { bankId: bank.id } });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-purple-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Загрузка...</p>
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
              {(((user?.user_metadata as any)?.full_name || user?.email || 'U') as string).charAt(0).toUpperCase()}
            </div>
            <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent">
              Z-Банк
            </h1>
            <div></div>
            <p className="text-gray-600 dark:text-gray-300 mt-[-23px]">{(user?.user_metadata as any)?.full_name || user?.email}</p>
          </div>
          <div className="flex items-center gap-3">
            <ThemeToggle />
            <Link to="/transactions" className="flex items-center gap-2 px-3 py-2 bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-500 dark:to-purple-500 rounded-lg shadow text-white hover:from-blue-700 hover:to-purple-700 transition hover:shadow-md">
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
                {getTotalBalance().toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ₽
              </p>
            </div>
          </div>
        </div>

        <div className="mb-8">
          <h2 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent mb-4">Ваши карты</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {banks.map((bank) => (
              <div key={bank.id} onClick={() => handleCardClick(bank)} className="cursor-pointer">
                <BankCard bank={bank} />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
