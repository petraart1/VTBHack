import { CreditCard } from 'lucide-react';
import { Bank } from '../types';

interface BankCardProps {
  bank: Bank;
}

export const BankCard = ({ bank }: BankCardProps) => {
  const formatCardNumber = (number: string) => {
    return `•••• ${number.slice(-4)}`;
  };

  const formatBalance = (balance: number, _currency: string) => {
    const sign = balance >= 0 ? '' : '-';
    const absBalance = Math.abs(balance);
    const amount = absBalance.toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return `${sign}${amount} ₽`;
  };

  const getGradient = (bankName: string) => {
    const name = bankName.toLowerCase();
    if (name.includes('альфа')) {
      return 'from-red-600 to-red-800';
    } else if (name.includes('сбер')) {
      return 'from-green-600 to-green-800';
    } else if (name.includes('втб')) {
      return 'from-blue-600 to-blue-800';
    } else if (name.includes('тинькофф')) {
      return 'from-gray-900 to-black';
    } else if (name.includes('райффайзен')) {
      return 'from-yellow-500 to-yellow-700';
    }
    // Fallback к старой системе цветов
    return 'from-blue-500 to-purple-600';
  };

  return (
    <div className={`bg-gradient-to-br ${getGradient(bank.bank_name)} rounded-2xl p-6 text-white shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105 dark:shadow-gray-900`}>
      <div className="flex justify-between items-start mb-8">
        <div>
          <p className="text-lg font-bold opacity-90 mb-1">{bank.bank_name}</p>
          <p className="text-xs opacity-75">{formatCardNumber(bank.card_number)}</p>
        </div>
        <div className="bg-white/20 p-2 rounded-lg backdrop-blur-sm">
          <CreditCard className="w-6 h-6" />
        </div>
      </div>

      <div className="flex justify-between items-end">
        <div>
          <p className="text-sm opacity-90 mb-1">Баланс</p>
          <p className={`text-2xl font-bold ${bank.balance < 0 ? 'text-red-200' : ''}`}>
            {formatBalance(bank.balance, bank.currency)}
          </p>
        </div>
        <div className="text-right">
          <p className="text-lg font-semibold opacity-90">{bank.card_type}</p>
        </div>
      </div>
    </div>
  );
};
