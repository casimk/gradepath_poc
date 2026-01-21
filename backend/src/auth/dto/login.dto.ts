import { IsString, IsEmail, MinLength, MaxLength, ValidateIf, Matches } from 'class-validator';

export class LoginDto {
  @ValidateIf((o) => !o.email)
  @IsString()
  @MinLength(3, { message: 'Username must be at least 3 characters' })
  @MaxLength(50, { message: 'Username must not exceed 50 characters' })
  @Matches(/^[a-zA-Z0-9_-]+$/, {
    message: 'Username can only contain letters, numbers, underscores, and hyphens',
  })
  username?: string;

  @ValidateIf((o) => !o.username)
  @IsEmail({}, { message: 'Invalid email address' })
  email?: string;

  @IsString()
  @MinLength(8, { message: 'Password must be at least 8 characters' })
  password: string;
}
