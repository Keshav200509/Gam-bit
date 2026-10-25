import React from 'react';
import { Shield, Skull, Zap, Award, User } from 'lucide-react';

interface UserAvatarProps {
  displayName: string;
  photoUrl?: string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export const UserAvatar: React.FC<UserAvatarProps> = ({
  displayName,
  photoUrl = 'avatar-1',
  size = 'md',
  className = ''
}) => {
  const sizeClasses = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-11 h-11 text-sm',
    lg: 'w-16 h-16 text-lg'
  };

  const iconSizes = {
    sm: 16,
    md: 20,
    lg: 28
  };

  const renderIcon = () => {
    switch (photoUrl) {
      case 'avatar-2':
        return <Zap size={iconSizes[size]} className="text-amber-400" />;
      case 'avatar-3':
        return <Skull size={iconSizes[size]} className="text-red-400" />;
      case 'avatar-4':
        return <Award size={iconSizes[size]} className="text-purple-400" />;
      case 'avatar-1':
      default:
        return <Shield size={iconSizes[size]} className="text-teal-400" />;
    }
  };

  return (
    <div
      className={`relative flex items-center justify-center rounded-xl bg-gradient-to-br from-[#1E2030] to-[#12131A] border border-[#2DD4BF]/30 shadow-lg shadow-teal-500/10 ${sizeClasses[size]} ${className}`}
    >
      {renderIcon()}
      <span className="sr-only">{displayName}</span>
    </div>
  );
};
