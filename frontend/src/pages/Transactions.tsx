import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { supabase } from '../lib/supabase';
import { Bank, Transaction } from '../types';
import { TransactionItem } from '../components/TransactionItem';
import { ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ThemeToggle } from '../components/ThemeToggle';

export const Transactions = () => {
  const { user } = useAuth();
  const [banks, setBanks] = useState<Bank[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);

  const [bankFilter, setBankFilter] = useState<string>('all');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [search, setSearch] = useState<string>('');

  useEffect(() => {
    const load = async () => {
      if (!user) return;
      setLoading(true);
      try {
        const { data: banksData } = await supabase
          .from('banks')
          .select('*')
          .eq('user_id', user.id)
          .order('created_at', { ascending: true });

        const { data: transactionsData } = await supabase
          .from('transactions')
          .select('*')
          .eq('user_id', user.id)
          .order('transaction_date', { ascending: false });

        setBanks(banksData || []);
        setTransactions(transactionsData || []);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user]);

  const getBankName = (bankId: string) => {
    const bank = banks.find(b => b.id === bankId);
    return bank?.bank_name || '';
  };

  const filtered = useMemo(() => {
    return transactions.filter((t) => {
      const bankOk = bankFilter === 'all' || t.bank_id === bankFilter;
      const typeOk = typeFilter === 'all' || t.transaction_type === typeFilter;
      const statusOk = statusFilter === 'all' || t.status === statusFilter;
      const searchOk = !search || t.description.toLowerCase().includes(search.toLowerCase());
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
            <Link to="/" className="flex items-center gap-2 px-3 py-2 bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-500 dark:to-purple-500 rounded-lg shadow text-white hover:from-blue-700 hover:to-purple-700 transition hover:shadow-md">
              <ArrowLeft className="w-4 h-4" />
              Назад
            </Link>
            <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent">Все операции</h1>
          </div>
          <ThemeToggle />
        </div>

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
              <option value="all">Все карты</option>
              {banks.map((b) => (
                <option key={b.id} value={b.id}>{b.bank_name}</option>
              ))}
            </select>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
            >
              <option value="all">Все типы</option>
              <option value="cash-in">Пополнение</option>
              <option value="cash-out">Снятие</option>
              <option value="transfer">Перевод</option>
              <option value="purchase">Покупка</option>
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
              <TransactionItem key={t.id} transaction={t} bankName={getBankName(t.bank_id)} />
            ))
          ) : (
            <div className="bg-white dark:bg-gray-800 rounded-xl p-8 text-center">
              <p className="text-gray-500 dark:text-gray-400">Операции не найдены</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

