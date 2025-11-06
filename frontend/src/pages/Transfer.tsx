import { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { supabase } from '../lib/supabase';
import { Bank } from '../types';
import { ArrowLeft } from 'lucide-react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { ThemeToggle } from '../components/ThemeToggle';

export const Transfer = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [banks, setBanks] = useState<Bank[]>([]);
  const [selectedBank, setSelectedBank] = useState<Bank | null>(null);
  const [loading, setLoading] = useState(true);
  const [transferForm, setTransferForm] = useState({
    recipientBank: '',
    cardNumber: '',
    amount: '',
  });
  const [transferError, setTransferError] = useState('');
  const [transferLoading, setTransferLoading] = useState(false);

  useEffect(() => {
    const loadBanks = async () => {
      if (!user) return;
      setLoading(true);
      try {
        const { data: banksData } = await supabase
          .from('banks')
          .select('*')
          .eq('user_id', user.id)
          .eq('is_active', true)
          .order('created_at', { ascending: true });

        setBanks(banksData || []);
        
        // Если передан bank_id в state, выбираем эту карту
        const bankId = location.state?.bankId;
        if (bankId && banksData) {
          const bank = banksData.find(b => b.id === bankId);
          if (bank) {
            setSelectedBank(bank);
          }
        }
      } finally {
        setLoading(false);
      }
    };
    loadBanks();
  }, [user, location.state]);

  const handleTransferSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBank || !user) return;
    
    setTransferError('');
    const amountNum = parseFloat(transferForm.amount);
    
    if (isNaN(amountNum) || amountNum <= 0) {
      setTransferError('Введите корректную сумму');
      return;
    }
    
    if (amountNum > selectedBank.balance) {
      setTransferError('Недостаточно средств на карте');
      return;
    }
    
    if (!transferForm.cardNumber || transferForm.cardNumber.length < 16) {
      setTransferError('Введите корректный номер карты (16 цифр)');
      return;
    }
    
    setTransferLoading(true);
    try {
      // Создаем транзакцию
      await supabase.from('transactions').insert({
        user_id: user.id,
        bank_id: selectedBank.id,
        transaction_type: 'transfer',
        amount: amountNum,
        description: `Перевод на карту ${transferForm.cardNumber.slice(-4)}`,
        status: 'confirmed',
        transaction_date: new Date().toISOString(),
      });
      
      // Обновляем баланс карты
      await supabase
        .from('banks')
        .update({ balance: selectedBank.balance - amountNum })
        .eq('id', selectedBank.id);
      
      navigate('/');
    } catch (error) {
      setTransferError('Ошибка при выполнении перевода');
      console.error(error);
    } finally {
      setTransferLoading(false);
    }
  };

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
      <div className="max-w-2xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <Link to="/" className="flex items-center gap-2 px-3 py-2 bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-500 dark:to-purple-500 rounded-lg shadow text-white hover:from-blue-700 hover:to-purple-700 transition hover:shadow-md">
              <ArrowLeft className="w-4 h-4" />
              Назад
            </Link>
            <h1 className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent">Перевод</h1>
          </div>
          <ThemeToggle />
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-lg p-6 border border-transparent hover:border-blue-500/20 dark:hover:border-purple-500/20 transition-colors">
          {selectedBank ? (
            <>
              <div className="mb-6 p-4 bg-gradient-to-br from-blue-50 to-purple-50 dark:from-blue-900/20 dark:to-purple-900/20 rounded-lg border border-blue-200/50 dark:border-purple-500/30">
                <p className="text-sm text-gray-600 dark:text-gray-300">Карта отправителя</p>
                <p className="font-semibold dark:text-white">{selectedBank.bank_name}</p>
                <p className="text-sm text-gray-600 dark:text-gray-300">Баланс: {selectedBank.balance.toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ₽</p>
              </div>

              {transferError && (
                <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded-lg mb-4">
                  {transferError}
                </div>
              )}

              <form onSubmit={handleTransferSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Банк получателя
                  </label>
                  <select
                    value={transferForm.recipientBank}
                    onChange={(e) => setTransferForm({ ...transferForm, recipientBank: e.target.value })}
                    className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
                  >
                    <option value="">Выберите банк</option>
                    <option value="Сбербанк">Сбербанк</option>
                    <option value="ВТБ">ВТБ</option>
                    <option value="Альфа-Банк">Альфа-Банк</option>
                    <option value="Тинькофф">Тинькофф</option>
                    <option value="Райффайзен">Райффайзен</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Номер карты получателя
                  </label>
                  <input
                    type="text"
                    value={transferForm.cardNumber}
                    onChange={(e) => setTransferForm({ ...transferForm, cardNumber: e.target.value.replace(/\D/g, '').slice(0, 16) })}
                    placeholder="1234 5678 9012 3456"
                    className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Сумма перевода
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={transferForm.amount}
                    onChange={(e) => setTransferForm({ ...transferForm, amount: e.target.value })}
                    placeholder="0.00"
                    max={selectedBank.balance}
                    className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
                    required
                  />
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Максимально: {selectedBank.balance.toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ₽
                  </p>
                </div>

                <button
                  type="submit"
                  disabled={transferLoading}
                  className="w-full bg-gradient-to-r from-blue-600 to-purple-600 text-white py-3 rounded-lg font-semibold hover:from-blue-700 hover:to-purple-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {transferLoading ? 'Выполнение...' : 'Перевести'}
                </button>
              </form>
            </>
          ) : (
            <>
              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Выберите карту отправителя
                </label>
                <select
                  value={selectedBank?.id || ''}
                  onChange={(e) => {
                    const bank = banks.find(b => b.id === e.target.value);
                    setSelectedBank(bank || null);
                  }}
                    className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
                >
                  <option value="">Выберите карту</option>
                  {banks.map((bank) => (
                    <option key={bank.id} value={bank.id}>
                      {bank.bank_name} - {bank.balance.toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ₽
                    </option>
                  ))}
                </select>
              </div>

              {selectedBank && (
                <>
                  <div className="mb-6 p-4 bg-gradient-to-br from-blue-50 to-purple-50 dark:from-blue-900/20 dark:to-purple-900/20 rounded-lg border border-blue-200/50 dark:border-purple-500/30">
                    <p className="text-sm text-gray-600 dark:text-gray-300">Карта отправителя</p>
                    <p className="font-semibold dark:text-white">{selectedBank.bank_name}</p>
                    <p className="text-sm text-gray-600 dark:text-gray-300">Баланс: {selectedBank.balance.toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ₽</p>
                  </div>

                  {transferError && (
                    <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded-lg mb-4">
                      {transferError}
                    </div>
                  )}

                  <form onSubmit={handleTransferSubmit} className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                        Банк получателя
                      </label>
                      <select
                        value={transferForm.recipientBank}
                        onChange={(e) => setTransferForm({ ...transferForm, recipientBank: e.target.value })}
                        className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
                      >
                        <option value="">Выберите банк</option>
                        <option value="Сбербанк">Сбербанк</option>
                        <option value="ВТБ">ВТБ</option>
                        <option value="Альфа-Банк">Альфа-Банк</option>
                        <option value="Тинькофф">Тинькофф</option>
                        <option value="Райффайзен">Райффайзен</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                        Номер карты получателя
                      </label>
                      <input
                        type="text"
                        value={transferForm.cardNumber}
                        onChange={(e) => setTransferForm({ ...transferForm, cardNumber: e.target.value.replace(/\D/g, '').slice(0, 16) })}
                        placeholder="1234 5678 9012 3456"
                        className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
                        required
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                        Сумма перевода
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        value={transferForm.amount}
                        onChange={(e) => setTransferForm({ ...transferForm, amount: e.target.value })}
                        placeholder="0.00"
                        max={selectedBank.balance}
                        className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:focus:border-purple-500 outline-none"
                        required
                      />
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                        Максимально: {selectedBank.balance.toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ₽
                      </p>
                    </div>

                    <button
                      type="submit"
                      disabled={transferLoading}
                      className="w-full bg-gradient-to-r from-blue-600 to-purple-600 text-white py-3 rounded-lg font-semibold hover:from-blue-700 hover:to-purple-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {transferLoading ? 'Выполнение...' : 'Перевести'}
                    </button>
                  </form>
                </>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

