import { Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ConfigService } from '@nestjs/config';
import { Strategy } from 'passport-facebook';

@Injectable()
export class FacebookOAuthStrategy extends PassportStrategy(Strategy, 'facebook') {
  constructor(private readonly configService: ConfigService) {
    super({
      clientID: configService.get('FACEBOOK_APP_ID') || '',
      clientSecret: configService.get('FACEBOOK_APP_SECRET') || '',
      callbackURL: configService.get('FACEBOOK_CALLBACK_URL') || 'http://localhost:3000/auth/oauth/facebook/callback',
      scope: ['email', 'public_profile'],
      profileFields: ['id', 'displayName', 'emails', 'name', 'photos'],
    });
  }

  async validate(accessToken: string, refreshToken: string, profile: any, done: (error: any, user?: any) => void): Promise<void> {
    const user = {
      provider: 'facebook',
      providerUserId: profile.id,
      email: profile.emails?.[0]?.value,
      displayName: profile.displayName,
      firstName: profile.name?.givenName,
      lastName: profile.name?.familyName,
      picture: profile.photos?.[0]?.value,
      profileData: profile,
    };

    done(null, user);
  }
}
