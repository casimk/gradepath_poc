import { ApiProperty } from '@nestjs/swagger';

export class AuthResponseDto {
  @ApiProperty({ description: 'User information' })
  user: {
    id: string;
    username?: string;
    email: string;
    displayName?: string;
  };

  @ApiProperty({ description: 'JWT access token' })
  accessToken: string;

  @ApiProperty({ description: 'Refresh token for obtaining new access tokens' })
  refreshToken: string;

  @ApiProperty({ description: 'Token expiration time in seconds', example: 900 })
  expiresIn: number;
}
